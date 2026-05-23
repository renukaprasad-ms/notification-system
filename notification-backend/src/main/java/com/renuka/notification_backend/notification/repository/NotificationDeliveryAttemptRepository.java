package com.renuka.notification_backend.notification.repository;

import com.renuka.notification_backend.notification.entity.NotificationDeliveryAttempt;
import com.renuka.notification_backend.notification.entity.DeliveryAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryAttemptRepository extends JpaRepository<NotificationDeliveryAttempt, UUID> {

    int countByNotificationRecipientId(UUID notificationRecipientId);

    int countByNotificationRecipientIdAndStatus(UUID notificationRecipientId, DeliveryAttemptStatus status);

    Optional<NotificationDeliveryAttempt> findTopByNotificationRecipientIdOrderByAttemptedAtDesc(UUID notificationRecipientId);
}
