package com.renuka.notification_backend.notification.dto;

import com.renuka.notification_backend.notification.entity.DeliveryStatus;
import com.renuka.notification_backend.notification.entity.NotificationPriority;
import com.renuka.notification_backend.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserNotificationSummaryResponse {

    private final UUID recipientId;
    private final UUID notificationId;
    private final String title;
    private final NotificationType type;
    private final NotificationPriority priority;
    private final DeliveryStatus deliveryStatus;
    private final LocalDateTime deliveredAt;
    private final LocalDateTime viewedAt;
    private final LocalDateTime readAt;
    private final LocalDateTime createdAt;

    public UserNotificationSummaryResponse(
            UUID recipientId,
            UUID notificationId,
            String title,
            NotificationType type,
            NotificationPriority priority,
            DeliveryStatus deliveryStatus,
            LocalDateTime deliveredAt,
            LocalDateTime viewedAt,
            LocalDateTime readAt,
            LocalDateTime createdAt
    ) {
        this.recipientId = recipientId;
        this.notificationId = notificationId;
        this.title = title;
        this.type = type;
        this.priority = priority;
        this.deliveryStatus = deliveryStatus;
        this.deliveredAt = deliveredAt;
        this.viewedAt = viewedAt;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public String getTitle() {
        return title;
    }

    public NotificationType getType() {
        return type;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
