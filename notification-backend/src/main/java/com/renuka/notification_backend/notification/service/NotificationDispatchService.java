package com.renuka.notification_backend.notification.service;

import com.renuka.notification_backend.notification.entity.NotificationRecipient;
import com.renuka.notification_backend.notification.realtime.NotificationPublishResult;
import com.renuka.notification_backend.notification.realtime.NotificationRedisPublisher;
import com.renuka.notification_backend.notification.realtime.NotificationStreamService;
import com.renuka.notification_backend.notification.repository.NotificationRecipientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationDispatchService {

    private static final int LIVE_PUBLISH_BATCH_SIZE = 250;

    private final NotificationRecipientRepository notificationRecipientRepository;
    private final NotificationQueueService notificationQueueService;
    private final NotificationRedisPublisher notificationRedisPublisher;
    private final NotificationStreamService notificationStreamService;
    private final NotificationDeliveryTrackingService notificationDeliveryTrackingService;
    private final UnreadCountCacheService unreadCountCacheService;
    private final boolean redisEnabled;
    private final boolean pubSubEnabled;

    public NotificationDispatchService(
            NotificationRecipientRepository notificationRecipientRepository,
            NotificationQueueService notificationQueueService,
            NotificationRedisPublisher notificationRedisPublisher,
            NotificationStreamService notificationStreamService,
            NotificationDeliveryTrackingService notificationDeliveryTrackingService,
            UnreadCountCacheService unreadCountCacheService,
            @Value("${app.redis.enabled:true}") boolean redisEnabled,
            @Value("${app.redis.pubsub.enabled:true}") boolean pubSubEnabled
    ) {
        this.notificationRecipientRepository = notificationRecipientRepository;
        this.notificationQueueService = notificationQueueService;
        this.notificationRedisPublisher = notificationRedisPublisher;
        this.notificationStreamService = notificationStreamService;
        this.notificationDeliveryTrackingService = notificationDeliveryTrackingService;
        this.unreadCountCacheService = unreadCountCacheService;
        this.redisEnabled = redisEnabled;
        this.pubSubEnabled = pubSubEnabled;
    }

    @Async("notificationDispatchExecutor")
    public void dispatchRecipients(List<NotificationRecipient> recipients) {
        if (notificationQueueService.enqueueRecipientIds(recipients.stream().map(NotificationRecipient::getId).toList())) {
            return;
        }

        publishAndTrack(recipients);
    }

    @Async("notificationDispatchExecutor")
    public void dispatchNotificationRecipients(UUID notificationId, boolean evictAllUnreadCounts) {
        if (evictAllUnreadCounts) {
            unreadCountCacheService.evictAllUnreadCounts();
        }

        int pageNumber = 0;
        Page<NotificationRecipient> page;

        do {
            page = notificationRecipientRepository.findByNotificationId(
                    notificationId,
                    PageRequest.of(pageNumber, LIVE_PUBLISH_BATCH_SIZE, Sort.by(Sort.Direction.ASC, "createdAt"))
            );
            List<NotificationRecipient> recipients = page.getContent();
            if (!notificationQueueService.enqueueRecipientIds(recipients.stream().map(NotificationRecipient::getId).toList())) {
                publishAndTrack(recipients);
            }
            pageNumber++;
        } while (page.hasNext());
    }

    private void publishAndTrack(List<NotificationRecipient> recipients) {
        if (recipients.isEmpty()) {
            return;
        }

        if (redisEnabled && pubSubEnabled && notificationRedisPublisher.publish(recipients)) {
            return;
        }

        List<NotificationPublishResult> results = notificationStreamService.publish(recipients);
        notificationDeliveryTrackingService.recordInAppResults(results);
    }
}
