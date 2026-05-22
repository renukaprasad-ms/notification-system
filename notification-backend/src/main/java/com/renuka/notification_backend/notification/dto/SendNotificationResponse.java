package com.renuka.notification_backend.notification.dto;

import java.util.UUID;

public class SendNotificationResponse {

    private final UUID notificationId;
    private final int recipientCount;

    public SendNotificationResponse(UUID notificationId, int recipientCount) {
        this.notificationId = notificationId;
        this.recipientCount = recipientCount;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public int getRecipientCount() {
        return recipientCount;
    }
}
