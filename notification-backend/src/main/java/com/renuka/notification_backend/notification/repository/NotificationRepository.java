package com.renuka.notification_backend.notification.repository;

import com.renuka.notification_backend.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    long countByCreatedById(UUID createdById);
}
