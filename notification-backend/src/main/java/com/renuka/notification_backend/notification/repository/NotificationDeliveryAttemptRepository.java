package com.renuka.notification_backend.notification.repository;

import com.renuka.notification_backend.notification.entity.NotificationDeliveryAttempt;
import com.renuka.notification_backend.notification.entity.DeliveryAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryAttemptRepository extends JpaRepository<NotificationDeliveryAttempt, UUID> {

    int countByNotificationRecipientId(UUID notificationRecipientId);

    int countByNotificationRecipientIdAndStatus(UUID notificationRecipientId, DeliveryAttemptStatus status);

    Optional<NotificationDeliveryAttempt> findTopByNotificationRecipientIdOrderByAttemptedAtDesc(UUID notificationRecipientId);

    @Modifying
    @Query(
            """
            delete from NotificationDeliveryAttempt nda
            where nda.notificationRecipient.id in :recipientIds
            """
    )
    int deleteAllByNotificationRecipientIdIn(@Param("recipientIds") List<UUID> recipientIds);
}
