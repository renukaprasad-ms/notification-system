package com.renuka.notification_backend.notification.dto;

import com.renuka.notification_backend.notification.entity.NotificationPriority;
import com.renuka.notification_backend.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SendAllNotificationRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotNull(message = "Notification priority is required")
    private NotificationPriority priority;

    @Size(max = 100, message = "Request id must not exceed 100 characters")
    private String requestId;

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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
