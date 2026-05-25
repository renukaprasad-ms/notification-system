package com.renuka.notification_backend.notification.service;

import com.renuka.notification_backend.notification.entity.NotificationRecipient;
import com.renuka.notification_backend.notification.realtime.NotificationPublishResult;
import com.renuka.notification_backend.notification.realtime.NotificationRedisPublisher;
import com.renuka.notification_backend.notification.realtime.NotificationStreamService;
import com.renuka.notification_backend.notification.repository.NotificationRecipientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationQueueWorker {

    private static final String RECIPIENT_ID_FIELD = "recipientId";

    private final NotificationQueueService notificationQueueService;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final NotificationRedisPublisher notificationRedisPublisher;
    private final NotificationStreamService notificationStreamService;
    private final NotificationDeliveryTrackingService notificationDeliveryTrackingService;
    private final boolean redisEnabled;
    private final boolean pubSubEnabled;
    private final int batchSize;
    private final Duration blockTimeout;
    private final String consumerName;

    public NotificationQueueWorker(
            NotificationQueueService notificationQueueService,
            NotificationRecipientRepository notificationRecipientRepository,
            NotificationRedisPublisher notificationRedisPublisher,
            NotificationStreamService notificationStreamService,
            NotificationDeliveryTrackingService notificationDeliveryTrackingService,
            @Value("${app.redis.enabled:true}") boolean redisEnabled,
            @Value("${app.redis.pubsub.enabled:true}") boolean pubSubEnabled,
            @Value("${app.notification.queue.batch-size:100}") int batchSize,
            @Value("${app.notification.queue.block-timeout-millis:1000}") long blockTimeoutMillis,
            @Value("${spring.application.name:notification-backend}") String applicationName
    ) {
        this.notificationQueueService = notificationQueueService;
        this.notificationRecipientRepository = notificationRecipientRepository;
        this.notificationRedisPublisher = notificationRedisPublisher;
        this.notificationStreamService = notificationStreamService;
        this.notificationDeliveryTrackingService = notificationDeliveryTrackingService;
        this.redisEnabled = redisEnabled;
        this.pubSubEnabled = pubSubEnabled;
        this.batchSize = batchSize;
        this.blockTimeout = Duration.ofMillis(blockTimeoutMillis);
        this.consumerName = applicationName + "-" + UUID.randomUUID();
    }

    @Scheduled(
            fixedDelayString = "${app.notification.queue.fixed-delay-millis:1000}",
            initialDelayString = "${app.notification.queue.initial-delay-millis:1000}"
    )
    public void processQueue() {
        if (!notificationQueueService.isEnabled()) {
            return;
        }

        List<MapRecord<String, Object, Object>> records = notificationQueueService.readBatch(consumerName, batchSize, blockTimeout);
        records.forEach(this::processRecord);
    }

    private void processRecord(MapRecord<String, Object, Object> record) {
        try {
            UUID recipientId = extractRecipientId(record);
            if (recipientId == null) {
                return;
            }

            Optional<NotificationRecipient> recipient = notificationRecipientRepository.findDetailedById(recipientId);
            if (recipient.isEmpty()) {
                return;
            }

            dispatchRecipient(recipient.get());
        } finally {
            notificationQueueService.acknowledgeAndDelete(record.getId());
        }
    }

    private void dispatchRecipient(NotificationRecipient recipient) {
        if (redisEnabled && pubSubEnabled && notificationRedisPublisher.publish(List.of(recipient))) {
            return;
        }

        NotificationPublishResult result = notificationStreamService.publish(List.of(recipient))
                .stream()
                .findFirst()
                .orElse(NotificationPublishResult.failed(recipient.getId(), "Notification queue publish failed"));

        notificationDeliveryTrackingService.recordInAppResult(result);
    }

    private UUID extractRecipientId(MapRecord<String, Object, Object> record) {
        Object value = record.getValue().get(RECIPIENT_ID_FIELD);
        if (value == null) {
            return null;
        }

        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
