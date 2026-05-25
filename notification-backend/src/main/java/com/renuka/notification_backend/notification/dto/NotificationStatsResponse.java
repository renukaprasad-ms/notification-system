package com.renuka.notification_backend.notification.dto;

public class NotificationStatsResponse {

    private final long totalNotifications;
    private final long unreadNotifications;
    private final long readNotifications;

    public NotificationStatsResponse(long totalNotifications, long unreadNotifications, long readNotifications) {
        this.totalNotifications = totalNotifications;
        this.unreadNotifications = unreadNotifications;
        this.readNotifications = readNotifications;
    }

    public long getTotalNotifications() {
        return totalNotifications;
    }

    public long getUnreadNotifications() {
        return unreadNotifications;
    }

    public long getReadNotifications() {
        return readNotifications;
    }
}
