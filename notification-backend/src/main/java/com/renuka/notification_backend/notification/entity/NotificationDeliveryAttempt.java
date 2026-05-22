package com.renuka.notification_backend.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "notification_delivery_attempts",
        indexes = {
                @Index(name = "idx_delivery_attempt_recipient", columnList = "notification_recipient_id"),
                @Index(name = "idx_delivery_attempt_status", columnList = "status")
        }
)
public class NotificationDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_recipient_id", nullable = false)
    private NotificationRecipient notificationRecipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryAttemptStatus status;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private LocalDateTime attemptedAt;

    @PrePersist
    void onCreate() {
        this.attemptedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public NotificationRecipient getNotificationRecipient() {
        return notificationRecipient;
    }

    public void setNotificationRecipient(NotificationRecipient notificationRecipient) {
        this.notificationRecipient = notificationRecipient;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public DeliveryAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryAttemptStatus status) {
        this.status = status;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }
}
