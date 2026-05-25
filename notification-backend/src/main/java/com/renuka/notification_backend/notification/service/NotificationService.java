package com.renuka.notification_backend.notification.service;

import com.renuka.notification_backend.common.exception.BadRequestException;
import com.renuka.notification_backend.common.exception.NotFoundException;
import com.renuka.notification_backend.common.exception.UnauthorizedException;
import com.renuka.notification_backend.common.response.PageResponse;
import com.renuka.notification_backend.common.utils.RedisRateLimitService;
import com.renuka.notification_backend.notification.dto.AdminNotificationOverviewResponse;
import com.renuka.notification_backend.notification.dto.SendAllNotificationRequest;
import com.renuka.notification_backend.notification.dto.SendNotificationResponse;
import com.renuka.notification_backend.notification.dto.SendSelectedNotificationRequest;
import com.renuka.notification_backend.notification.dto.UnreadCountResponse;
import com.renuka.notification_backend.notification.dto.UserNotificationResponse;
import com.renuka.notification_backend.notification.dto.UserNotificationSummaryResponse;
import com.renuka.notification_backend.notification.entity.Notification;
import com.renuka.notification_backend.notification.entity.NotificationRecipient;
import com.renuka.notification_backend.notification.realtime.NotificationStreamService;
import com.renuka.notification_backend.notification.repository.NotificationDeliveryAttemptRepository;
import com.renuka.notification_backend.notification.repository.NotificationRecipientRepository;
import com.renuka.notification_backend.notification.repository.NotificationRepository;
import com.renuka.notification_backend.user.entity.User;
import com.renuka.notification_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {

    private static final String MATCH_ALL_SEARCH = "__all__";

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final NotificationDeliveryAttemptRepository notificationDeliveryAttemptRepository;
    private final UserRepository userRepository;
    private final NotificationStreamService notificationStreamService;
    private final NotificationDispatchService notificationDispatchService;
    private final UnreadCountCacheService unreadCountCacheService;
    private final RedisRateLimitService redisRateLimitService;
    private final long adminSendLimit;
    private final Duration adminSendWindow;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationRecipientRepository notificationRecipientRepository,
            NotificationDeliveryAttemptRepository notificationDeliveryAttemptRepository,
            UserRepository userRepository,
            NotificationStreamService notificationStreamService,
            NotificationDispatchService notificationDispatchService,
            UnreadCountCacheService unreadCountCacheService,
            RedisRateLimitService redisRateLimitService,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.notification.admin.limit:20}") long adminSendLimit,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.notification.admin.window-minutes:1}") long adminSendWindowMinutes
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationRecipientRepository = notificationRecipientRepository;
        this.notificationDeliveryAttemptRepository = notificationDeliveryAttemptRepository;
        this.userRepository = userRepository;
        this.notificationStreamService = notificationStreamService;
        this.notificationDispatchService = notificationDispatchService;
        this.unreadCountCacheService = unreadCountCacheService;
        this.redisRateLimitService = redisRateLimitService;
        this.adminSendLimit = adminSendLimit;
        this.adminSendWindow = Duration.ofMinutes(adminSendWindowMinutes);
    }

    @Transactional
    public SendNotificationResponse sendToAllUsers(SendAllNotificationRequest request, String adminEmail) {
        assertAdminSendAllowed(adminEmail);
        User admin = getActiveUser(adminEmail);
        Optional<SendNotificationResponse> existingResponse = findExistingResponse(admin, request.getRequestId());
        if (existingResponse.isPresent()) {
            return existingResponse.get();
        }
        long activeUsers = userRepository.countByActiveTrue();

        if (activeUsers == 0) {
            throw new BadRequestException("No active users found");
        }

        return createNotificationForAllActiveUsers(request, admin);
    }

    @Transactional
    public SendNotificationResponse sendToSelectedUsers(SendSelectedNotificationRequest request, String adminEmail) {
        assertAdminSendAllowed(adminEmail);
        User admin = getActiveUser(adminEmail);
        Optional<SendNotificationResponse> existingResponse = findExistingResponse(admin, request.getRequestId());
        if (existingResponse.isPresent()) {
            return existingResponse.get();
        }
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
        Notification savedNotification = saveNotification(request, createdBy);

        List<NotificationRecipient> notificationRecipients = recipients.stream()
                .map(user -> toRecipient(savedNotification, user))
                .toList();

        List<NotificationRecipient> savedRecipients = notificationRecipientRepository.saveAll(notificationRecipients);
        unreadCountCacheService.incrementUnreadCounts(savedRecipients.stream().map(recipient -> recipient.getUser().getId()).toList());
        publishAfterCommit(savedRecipients);

        return new SendNotificationResponse(savedNotification.getId(), savedRecipients.size());
    }

    private SendNotificationResponse createNotificationForAllActiveUsers(
            SendAllNotificationRequest request,
            User createdBy
    ) {
        Notification savedNotification = saveNotification(request, createdBy);
        int recipientCount = notificationRecipientRepository.insertPendingRecipientsForActiveUsers(savedNotification.getId());
        publishAfterCommit(savedNotification.getId(), true);

        return new SendNotificationResponse(savedNotification.getId(), recipientCount);
    }

    private Notification saveNotification(SendAllNotificationRequest request, User createdBy) {
        Notification notification = new Notification();
        notification.setTitle(request.getTitle().trim());
        notification.setMessage(request.getMessage().trim());
        notification.setType(request.getType());
        notification.setPriority(request.getPriority());
        notification.setRequestId(normalizeRequestId(request.getRequestId()));
        notification.setCreatedBy(createdBy);

        return notificationRepository.save(notification);
    }

    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamNotifications(String userEmail) {
        User user = getActiveUser(userEmail);
        return notificationStreamService.subscribe(user, "default");
    }

    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamNotifications(String userEmail, String clientId) {
        User user = getActiveUser(userEmail);
        if (clientId == null || clientId.isBlank()) {
            throw new BadRequestException("Client id is required for notification streaming");
        }

        return notificationStreamService.subscribe(user, clientId.trim());
    }

    @Transactional(readOnly = true)
    public PageResponse<UserNotificationSummaryResponse> getMyNotifications(String userEmail, int page, int size, String search) {
        User user = getActiveUser(userEmail);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        String normalizedSearch = normalizeSearch(search);

        Page<NotificationRecipient> pageResult = MATCH_ALL_SEARCH.equals(normalizedSearch)
                ? notificationRecipientRepository.findByUserId(user.getId(), pageable)
                : notificationRecipientRepository.findUserNotifications(user.getId(), normalizedSearch, pageable);

        return PageResponse.from(pageResult.map(this::toUserNotificationSummaryResponse));
    }

    @Transactional
    public UserNotificationResponse getMyNotificationById(UUID recipientId, String userEmail) {
        User user = getActiveUser(userEmail);
        NotificationRecipient recipient = getOwnedRecipient(recipientId, user);
        LocalDateTime now = LocalDateTime.now();
        boolean unreadBeforeOpen = recipient.getReadAt() == null;

        if (recipient.getViewedAt() == null) {
            recipient.setViewedAt(now);
        }

        if (recipient.getReadAt() == null) {
            recipient.setReadAt(now);
        }

        NotificationRecipient updatedRecipient = notificationRecipientRepository.save(recipient);
        if (unreadBeforeOpen) {
            unreadCountCacheService.decrementUnreadCount(user.getId());
        }

        return toUserNotificationResponse(updatedRecipient);
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
            unreadCountCacheService.decrementUnreadCount(user.getId());
        }

        return toUserNotificationResponse(notificationRecipientRepository.save(recipient));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(String userEmail) {
        User user = getActiveUser(userEmail);
        long unreadCount = unreadCountCacheService.getUnreadCount(
                user.getId(),
                () -> notificationRecipientRepository.countByUserIdAndReadAtIsNull(user.getId())
        );
        return new UnreadCountResponse(unreadCount);
    }

    @Transactional(readOnly = true)
    public AdminNotificationOverviewResponse getAdminOverview(String adminEmail) {
        User admin = getActiveUser(adminEmail);
        long notificationsSent = notificationRepository.countByCreatedById(admin.getId());
        long activeUsers = userRepository.countByActiveTrue();

        return new AdminNotificationOverviewResponse(notificationsSent, activeUsers);
    }

    @Transactional
    public void deleteNotification(UUID notificationId, String adminEmail) {
        getActiveUser(adminEmail);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        List<UUID> unreadUserIds = notificationRecipientRepository.findUnreadUserIdsByNotificationId(notificationId);
        List<UUID> recipientIds = notificationRecipientRepository.findIdsByNotificationId(notificationId);

        if (!recipientIds.isEmpty()) {
            notificationDeliveryAttemptRepository.deleteAllByNotificationRecipientIdIn(recipientIds);
        }

        notificationRecipientRepository.deleteAllByNotificationId(notificationId);
        notificationRepository.delete(notification);
        unreadUserIds.forEach(unreadCountCacheService::evictUnreadCount);
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

    private UserNotificationSummaryResponse toUserNotificationSummaryResponse(NotificationRecipient recipient) {
        Notification notification = recipient.getNotification();
        return new UserNotificationSummaryResponse(
                recipient.getId(),
                notification.getId(),
                notification.getTitle(),
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
            notificationDispatchService.dispatchRecipients(recipients);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationDispatchService.dispatchRecipients(recipients);
            }
        });
    }

    private void publishAfterCommit(UUID notificationId, boolean evictAllUnreadCounts) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationDispatchService.dispatchNotificationRecipients(notificationId, evictAllUnreadCounts);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationDispatchService.dispatchNotificationRecipients(notificationId, evictAllUnreadCounts);
            }
        });
    }

    private void assertAdminSendAllowed(String adminEmail) {
        redisRateLimitService.assertAllowed(
                "rate-limit:notification:admin-send",
                adminEmail,
                adminSendLimit,
                adminSendWindow,
                "Too many notification send attempts. Please try again later."
        );
    }

    private Optional<SendNotificationResponse> findExistingResponse(User admin, String requestId) {
        String normalizedRequestId = normalizeRequestId(requestId);
        if (normalizedRequestId == null) {
            return Optional.empty();
        }

        return notificationRepository.findByCreatedByIdAndRequestId(admin.getId(), normalizedRequestId)
                .map(notification -> new SendNotificationResponse(
                        notification.getId(),
                        Math.toIntExact(notificationRecipientRepository.countByNotificationId(notification.getId()))
                ));
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }

        return requestId.trim();
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return MATCH_ALL_SEARCH;
        }

        return search.trim();
    }

}
