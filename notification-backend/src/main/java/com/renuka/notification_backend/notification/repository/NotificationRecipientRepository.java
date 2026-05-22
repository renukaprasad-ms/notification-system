package com.renuka.notification_backend.notification.repository;

import com.renuka.notification_backend.notification.entity.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, UUID> {

    List<NotificationRecipient> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<NotificationRecipient> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndReadAtIsNull(UUID userId);
}
