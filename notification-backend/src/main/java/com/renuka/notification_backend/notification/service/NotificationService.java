package com.renuka.notification_backend.notification.service;

import com.renuka.notification_backend.common.exception.BadRequestException;
import com.renuka.notification_backend.common.exception.NotFoundException;
import com.renuka.notification_backend.common.exception.UnauthorizedException;
import com.renuka.notification_backend.notification.dto.SendAllNotificationRequest;
import com.renuka.notification_backend.notification.dto.SendNotificationResponse;
import com.renuka.notification_backend.notification.dto.SendSelectedNotificationRequest;
import com.renuka.notification_backend.notification.dto.UnreadCountResponse;
import com.renuka.notification_backend.notification.dto.UserNotificationResponse;
import com.renuka.notification_backend.notification.entity.Notification;
import com.renuka.notification_backend.notification.entity.NotificationRecipient;
import com.renuka.notification_backend.notification.realtime.NotificationPublishResult;
import com.renuka.notification_backend.notification.realtime.NotificationStreamService;
import com.renuka.notification_backend.notification.repository.NotificationRecipientRepository;
import com.renuka.notification_backend.notification.repository.NotificationRepository;
import com.renuka.notification_backend.user.entity.User;
import com.renuka.notification_backend.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final UserRepository userRepository;
    private final NotificationStreamService notificationStreamService;
    private final NotificationDeliveryTrackingService notificationDeliveryTrackingService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationRecipientRepository notificationRecipientRepository,
            UserRepository userRepository,
            NotificationStreamService notificationStreamService,
            NotificationDeliveryTrackingService notificationDeliveryTrackingService
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationRecipientRepository = notificationRecipientRepository;
        this.userRepository = userRepository;
        this.notificationStreamService = notificationStreamService;
        this.notificationDeliveryTrackingService = notificationDeliveryTrackingService;
    }

    @Transactional
    public SendNotificationResponse sendToAllUsers(SendAllNotificationRequest request, String adminEmail) {
        User admin = getActiveUser(adminEmail);
        List<User> recipients = userRepository.findByActiveTrue();

        if (recipients.isEmpty()) {
            throw new BadRequestException("No active users found");
        }

        return createNotification(request, admin, recipients);
    }

    @Transactional
    public SendNotificationResponse sendToSelectedUsers(SendSelectedNotificationRequest request, String adminEmail) {
        User admin = getActiveUser(adminEmail);
        List<UUID> recipientIds = distinctRecipientIds(request.getRecipientUserIds());

        if (recipientIds.isEmpty()) {
            throw new BadRequestException("Recipient user ids are required");
        }

        List<User> recipients = userRepository.findByIdInAndActiveTrue(recipientIds);
        if (recipients.size() != recipientIds.size()) {
            throw new BadRequestException("One or more recipient users are invalid or inactive");
        }

        return createNotification(request, admin, recipients);
    }

    private SendNotificationResponse createNotification(
            SendAllNotificationRequest request,
            User createdBy,
            List<User> recipients
    ) {
        Notification notification = new Notification();
        notification.setTitle(request.getTitle().trim());
        notification.setMessage(request.getMessage().trim());
        notification.setType(request.getType());
        notification.setPriority(request.getPriority());
        notification.setCreatedBy(createdBy);

        Notification savedNotification = notificationRepository.save(notification);

        List<NotificationRecipient> notificationRecipients = recipients.stream()
                .map(user -> toRecipient(savedNotification, user))
                .toList();

        List<NotificationRecipient> savedRecipients = notificationRecipientRepository.saveAll(notificationRecipients);
        publishAfterCommit(savedRecipients);

        return new SendNotificationResponse(savedNotification.getId(), savedRecipients.size());
    }

    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamNotifications(String userEmail) {
        User user = getActiveUser(userEmail);
        return notificationStreamService.subscribe(user);
    }

    @Transactional
    public List<UserNotificationResponse> getMyNotifications(String userEmail) {
        User user = getActiveUser(userEmail);

        return notificationRecipientRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toUserNotificationResponse)
                .toList();
    }

    @Transactional
    public UserNotificationResponse markViewed(UUID recipientId, String userEmail) {
        User user = getActiveUser(userEmail);
        NotificationRecipient recipient = getOwnedRecipient(recipientId, user);

        if (recipient.getViewedAt() == null) {
            recipient.setViewedAt(LocalDateTime.now());
            recipient = notificationRecipientRepository.save(recipient);
        }

        return toUserNotificationResponse(recipient);
    }

    @Transactional
    public UserNotificationResponse markRead(UUID recipientId, String userEmail) {
        User user = getActiveUser(userEmail);
        NotificationRecipient recipient = getOwnedRecipient(recipientId, user);
        LocalDateTime now = LocalDateTime.now();

        if (recipient.getViewedAt() == null) {
            recipient.setViewedAt(now);
        }

        if (recipient.getReadAt() == null) {
            recipient.setReadAt(now);
        }

        return toUserNotificationResponse(notificationRecipientRepository.save(recipient));
    }

    @Transactional
    public UnreadCountResponse getUnreadCount(String userEmail) {
        User user = getActiveUser(userEmail);
        return new UnreadCountResponse(notificationRecipientRepository.countByUserIdAndReadAtIsNull(user.getId()));
    }

    private NotificationRecipient toRecipient(Notification notification, User user) {
        NotificationRecipient recipient = new NotificationRecipient();
        recipient.setNotification(notification);
        recipient.setUser(user);
        return recipient;
    }

    private User getActiveUser(String email) {
        return userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException("Authentication is required"));
    }

    private NotificationRecipient getOwnedRecipient(UUID recipientId, User user) {
        return notificationRecipientRepository.findByIdAndUserId(recipientId, user.getId())
                .orElseThrow(() -> new NotFoundException("Notification not found"));
    }

    private UserNotificationResponse toUserNotificationResponse(NotificationRecipient recipient) {
        Notification notification = recipient.getNotification();
        return new UserNotificationResponse(
                recipient.getId(),
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getPriority(),
                recipient.getDeliveryStatus(),
                recipient.getDeliveredAt(),
                recipient.getViewedAt(),
                recipient.getReadAt(),
                recipient.getCreatedAt()
        );
    }

    private List<UUID> distinctRecipientIds(List<UUID> recipientUserIds) {
        if (recipientUserIds == null) {
            return List.of();
        }

        Set<UUID> distinctIds = new LinkedHashSet<>(recipientUserIds);
        distinctIds.remove(null);
        return List.copyOf(distinctIds);
    }

    private void publishAfterCommit(List<NotificationRecipient> recipients) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishAndTrack(recipients);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishAndTrack(recipients);
            }
        });
    }

    private void publishAndTrack(List<NotificationRecipient> recipients) {
        List<NotificationPublishResult> results = notificationStreamService.publish(recipients);
        notificationDeliveryTrackingService.recordInAppResults(results);
    }
}
