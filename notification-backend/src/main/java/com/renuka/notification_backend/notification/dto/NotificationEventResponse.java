package com.renuka.notification_backend.notification.dto;

import com.renuka.notification_backend.notification.entity.NotificationPriority;
import com.renuka.notification_backend.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationEventResponse {

    private final UUID recipientId;
    private final UUID notificationId;
    private final String title;
    private final String message;
    private final NotificationType type;
    private final NotificationPriority priority;
    private final LocalDateTime createdAt;

    public NotificationEventResponse(
            UUID recipientId,
            UUID notificationId,
            String title,
            String message,
            NotificationType type,
            NotificationPriority priority,
            LocalDateTime createdAt
    ) {
        this.recipientId = recipientId;
        this.notificationId = notificationId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.priority = priority;
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

    public String getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
