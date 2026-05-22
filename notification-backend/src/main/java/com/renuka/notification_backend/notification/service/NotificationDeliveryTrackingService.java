package com.renuka.notification_backend.notification.service;

import com.renuka.notification_backend.notification.entity.DeliveryAttemptStatus;
import com.renuka.notification_backend.notification.entity.DeliveryStatus;
import com.renuka.notification_backend.notification.entity.NotificationChannel;
import com.renuka.notification_backend.notification.entity.NotificationDeliveryAttempt;
import com.renuka.notification_backend.notification.entity.NotificationRecipient;
import com.renuka.notification_backend.notification.realtime.NotificationPublishResult;
import com.renuka.notification_backend.notification.repository.NotificationDeliveryAttemptRepository;
import com.renuka.notification_backend.notification.repository.NotificationRecipientRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationDeliveryTrackingService {

    private final NotificationRecipientRepository notificationRecipientRepository;
    private final NotificationDeliveryAttemptRepository notificationDeliveryAttemptRepository;

    public NotificationDeliveryTrackingService(
            NotificationRecipientRepository notificationRecipientRepository,
            NotificationDeliveryAttemptRepository notificationDeliveryAttemptRepository
    ) {
        this.notificationRecipientRepository = notificationRecipientRepository;
        this.notificationDeliveryAttemptRepository = notificationDeliveryAttemptRepository;
    }

    @Transactional
    public void recordInAppResults(List<NotificationPublishResult> results) {
        for (NotificationPublishResult result : results) {
            notificationRecipientRepository.findById(result.getRecipientId())
                    .ifPresent(recipient -> recordResult(recipient, result));
        }
    }

    private void recordResult(NotificationRecipient recipient, NotificationPublishResult result) {
        if (result.isDelivered()) {
            recipient.setDeliveryStatus(DeliveryStatus.DELIVERED);
            recipient.setDeliveredAt(LocalDateTime.now());
        } else {
            recipient.setDeliveryStatus(DeliveryStatus.FAILED);
        }

        notificationRecipientRepository.save(recipient);
        notificationDeliveryAttemptRepository.save(toAttempt(recipient, result));
    }

    private NotificationDeliveryAttempt toAttempt(NotificationRecipient recipient, NotificationPublishResult result) {
        NotificationDeliveryAttempt attempt = new NotificationDeliveryAttempt();
        attempt.setNotificationRecipient(recipient);
        attempt.setChannel(NotificationChannel.IN_APP);
        attempt.setStatus(result.isDelivered() ? DeliveryAttemptStatus.SUCCESS : DeliveryAttemptStatus.FAILED);
        attempt.setAttemptNumber(nextAttemptNumber(recipient));
        attempt.setErrorMessage(result.getErrorMessage());
        return attempt;
    }

    private int nextAttemptNumber(NotificationRecipient recipient) {
        return notificationDeliveryAttemptRepository.countByNotificationRecipientId(recipient.getId()) + 1;
    }
}
