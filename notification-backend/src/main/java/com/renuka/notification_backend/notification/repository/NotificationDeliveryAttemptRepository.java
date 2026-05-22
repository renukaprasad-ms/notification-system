package com.renuka.notification_backend.notification.repository;

import com.renuka.notification_backend.notification.entity.NotificationDeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationDeliveryAttemptRepository extends JpaRepository<NotificationDeliveryAttempt, UUID> {

    int countByNotificationRecipientId(UUID notificationRecipientId);
}
