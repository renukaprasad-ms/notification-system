package com.renuka.notification_backend.notification.realtime;

import com.renuka.notification_backend.notification.entity.NotificationPriority;
import com.renuka.notification_backend.notification.entity.NotificationRecipient;
import com.renuka.notification_backend.notification.entity.NotificationType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationRedisMessage implements Serializable {

    private UUID recipientId;
    private UUID userId;
    private UUID notificationId;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationPriority priority;
    private LocalDateTime createdAt;

    public static NotificationRedisMessage fromRecipient(NotificationRecipient recipient) {
        NotificationRedisMessage redisMessage = new NotificationRedisMessage();
        redisMessage.setRecipientId(recipient.getId());
        redisMessage.setUserId(recipient.getUser().getId());
        redisMessage.setNotificationId(recipient.getNotification().getId());
        redisMessage.setTitle(recipient.getNotification().getTitle());
        redisMessage.setMessage(recipient.getNotification().getMessage());
        redisMessage.setType(recipient.getNotification().getType());
        redisMessage.setPriority(recipient.getNotification().getPriority());
        redisMessage.setCreatedAt(recipient.getNotification().getCreatedAt());
        return redisMessage;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(UUID recipientId) {
        this.recipientId = recipientId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public void setPriority(NotificationPriority priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
