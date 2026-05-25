package com.renuka.notification_backend.notification.service;

import com.renuka.notification_backend.common.utils.RedisCoordinationService;
import com.renuka.notification_backend.notification.entity.DeliveryAttemptStatus;
import com.renuka.notification_backend.notification.entity.DeliveryStatus;
import com.renuka.notification_backend.notification.entity.NotificationRecipient;
import com.renuka.notification_backend.notification.realtime.NotificationPublishResult;
import com.renuka.notification_backend.notification.realtime.NotificationRedisPublisher;
import com.renuka.notification_backend.notification.realtime.NotificationStreamService;
import com.renuka.notification_backend.notification.repository.NotificationDeliveryAttemptRepository;
import com.renuka.notification_backend.notification.repository.NotificationRecipientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.notification.retry.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationRetryWorker {

    private final NotificationRecipientRepository notificationRecipientRepository;
    private final NotificationDeliveryAttemptRepository notificationDeliveryAttemptRepository;
    private final NotificationQueueService notificationQueueService;
    private final NotificationRedisPublisher notificationRedisPublisher;
    private final NotificationStreamService notificationStreamService;
    private final NotificationDeliveryTrackingService notificationDeliveryTrackingService;
    private final RedisCoordinationService redisCoordinationService;
    private final boolean redisEnabled;
    private final boolean pubSubEnabled;
    private final int maxAttempts;
    private final int batchSize;
    private final Duration retryLockTtl;

    public NotificationRetryWorker(
            NotificationRecipientRepository notificationRecipientRepository,
            NotificationDeliveryAttemptRepository notificationDeliveryAttemptRepository,
            NotificationQueueService notificationQueueService,
            NotificationRedisPublisher notificationRedisPublisher,
            NotificationStreamService notificationStreamService,
            NotificationDeliveryTrackingService notificationDeliveryTrackingService,
            RedisCoordinationService redisCoordinationService,
            @Value("${app.redis.enabled:true}") boolean redisEnabled,
            @Value("${app.redis.pubsub.enabled:true}") boolean pubSubEnabled,
            @Value("${app.notification.retry.max-attempts:3}") int maxAttempts,
            @Value("${app.notification.retry.batch-size:25}") int batchSize,
            @Value("${app.notification.retry.lock-ttl-seconds:45}") long retryLockTtlSeconds
    ) {
        this.notificationRecipientRepository = notificationRecipientRepository;
        this.notificationDeliveryAttemptRepository = notificationDeliveryAttemptRepository;
        this.notificationQueueService = notificationQueueService;
        this.notificationRedisPublisher = notificationRedisPublisher;
        this.notificationStreamService = notificationStreamService;
        this.notificationDeliveryTrackingService = notificationDeliveryTrackingService;
        this.redisCoordinationService = redisCoordinationService;
        this.redisEnabled = redisEnabled;
        this.pubSubEnabled = pubSubEnabled;
        this.maxAttempts = maxAttempts;
        this.batchSize = batchSize;
        this.retryLockTtl = Duration.ofSeconds(retryLockTtlSeconds);
    }

    @Scheduled(
            fixedDelayString = "${app.notification.retry.fixed-delay-millis:30000}",
            initialDelayString = "${app.notification.retry.initial-delay-millis:15000}"
    )
    public void retryPendingAndFailedDeliveries() {
        List<NotificationRecipient> recipients = notificationRecipientRepository.findByDeliveryStatusInOrderByCreatedAtAsc(
                List.of(DeliveryStatus.PENDING, DeliveryStatus.FAILED),
                PageRequest.of(0, batchSize)
        );

        for (NotificationRecipient recipient : recipients) {
            if (notificationDeliveryAttemptRepository.countByNotificationRecipientIdAndStatus(
                    recipient.getId(),
                    DeliveryAttemptStatus.FAILED
            ) >= maxAttempts) {
                continue;
            }

            String lockKey = retryLockKey(recipient.getId());
            if (!redisCoordinationService.acquireLock(lockKey, retryLockTtl)) {
                continue;
            }

            try {
                retryRecipient(recipient);
            } finally {
                redisCoordinationService.releaseLock(lockKey);
            }
        }
    }

    private void retryRecipient(NotificationRecipient recipient) {
        if (notificationQueueService.enqueueRecipientIds(List.of(recipient.getId()))) {
            return;
        }

        if (redisEnabled && pubSubEnabled && notificationRedisPublisher.publish(List.of(recipient))) {
            return;
        }

        NotificationPublishResult result = notificationStreamService.publish(List.of(recipient))
                .stream()
                .findFirst()
                .orElse(NotificationPublishResult.failed(recipient.getId(), "Retry publish failed"));

        notificationDeliveryTrackingService.recordInAppResult(result);
    }

    private String retryLockKey(UUID recipientId) {
        return "notification:retry:lock:" + recipientId;
    }
}
