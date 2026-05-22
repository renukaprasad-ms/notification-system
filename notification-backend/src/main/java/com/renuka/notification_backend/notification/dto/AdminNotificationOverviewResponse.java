package com.renuka.notification_backend.notification.dto;

public class AdminNotificationOverviewResponse {

    private final long notificationsSent;
    private final long activeUsers;

    public AdminNotificationOverviewResponse(long notificationsSent, long activeUsers) {
        this.notificationsSent = notificationsSent;
        this.activeUsers = activeUsers;
    }

    public long getNotificationsSent() {
        return notificationsSent;
    }

    public long getActiveUsers() {
        return activeUsers;
    }
}
