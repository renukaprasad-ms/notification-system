package com.renuka.notification_backend.notification.realtime;

import java.util.UUID;

public class NotificationPublishResult {

    private final UUID recipientId;
    private final boolean delivered;
    private final String errorMessage;

    private NotificationPublishResult(UUID recipientId, boolean delivered, String errorMessage) {
        this.recipientId = recipientId;
        this.delivered = delivered;
        this.errorMessage = errorMessage;
    }

    public static NotificationPublishResult delivered(UUID recipientId) {
        return new NotificationPublishResult(recipientId, true, null);
    }

    public static NotificationPublishResult failed(UUID recipientId, String errorMessage) {
        return new NotificationPublishResult(recipientId, false, errorMessage);
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
