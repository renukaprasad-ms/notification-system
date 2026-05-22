package com.renuka.notification_backend.notification.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public class SendSelectedNotificationRequest extends SendAllNotificationRequest {

    @NotEmpty(message = "Recipient user ids are required")
    private List<UUID> recipientUserIds;

    public List<UUID> getRecipientUserIds() {
        return recipientUserIds;
    }

    public void setRecipientUserIds(List<UUID> recipientUserIds) {
        this.recipientUserIds = recipientUserIds;
    }
}
