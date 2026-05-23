package com.renuka.notification_backend.notification.realtime;

import com.renuka.notification_backend.notification.service.NotificationDeliveryTrackingService;
import org.springframework.stereotype.Service;

@Service
public class NotificationRedisSubscriber {

    private final NotificationStreamService notificationStreamService;
    private final NotificationDeliveryTrackingService notificationDeliveryTrackingService;

    public NotificationRedisSubscriber(
            NotificationStreamService notificationStreamService,
            NotificationDeliveryTrackingService notificationDeliveryTrackingService
    ) {
        this.notificationStreamService = notificationStreamService;
        this.notificationDeliveryTrackingService = notificationDeliveryTrackingService;
    }

    public void handleMessage(NotificationRedisMessage message) {
        NotificationPublishResult publishResult = notificationStreamService.publish(message, true);
        if (publishResult != null) {
            notificationDeliveryTrackingService.recordInAppResult(publishResult);
        }
    }
}
