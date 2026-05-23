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
import com.renuka.notification_backend.notification.entity.Notification;
import com.renuka.notification_backend.notification.entity.NotificationRecipient;
import com.renuka.notification_backend.notification.realtime.NotificationPublishResult;
import com.renuka.notification_backend.notification.realtime.NotificationRedisPublisher;
import com.renuka.notification_backend.notification.realtime.NotificationStreamService;
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
    private final UserRepository userRepository;
    private final NotificationStreamService notificationStreamService;
    private final NotificationRedisPublisher notificationRedisPublisher;
    private final NotificationDeliveryTrackingService notificationDeliveryTrackingService;
    private final UnreadCountCacheService unreadCountCacheService;
    private final RedisRateLimitService redisRateLimitService;
    private final boolean redisEnabled;
    private final boolean pubSubEnabled;
    private final long adminSendLimit;
    private final Duration adminSendWindow;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationRecipientRepository notificationRecipientRepository,
            UserRepository userRepository,
            NotificationStreamService notificationStreamService,
            NotificationRedisPublisher notificationRedisPublisher,
            NotificationDeliveryTrackingService notificationDeliveryTrackingService,
            UnreadCountCacheService unreadCountCacheService,
            RedisRateLimitService redisRateLimitService,
            @org.springframework.beans.factory.annotation.Value("${app.redis.enabled:true}") boolean redisEnabled,
            @org.springframework.beans.factory.annotation.Value("${app.redis.pubsub.enabled:true}") boolean pubSubEnabled,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.notification.admin.limit:20}") long adminSendLimit,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.notification.admin.window-minutes:1}") long adminSendWindowMinutes
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationRecipientRepository = notificationRecipientRepository;
        this.userRepository = userRepository;
        this.notificationStreamService = notificationStreamService;
        this.notificationRedisPublisher = notificationRedisPublisher;
        this.notificationDeliveryTrackingService = notificationDeliveryTrackingService;
        this.unreadCountCacheService = unreadCountCacheService;
        this.redisRateLimitService = redisRateLimitService;
        this.redisEnabled = redisEnabled;
        this.pubSubEnabled = pubSubEnabled;
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
        unreadCountCacheService.evictAllUnreadCounts();

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
    public PageResponse<UserNotificationResponse> getMyNotifications(String userEmail, int page, int size, String search) {
        User user = getActiveUser(userEmail);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        String normalizedSearch = normalizeSearch(search);

        Page<NotificationRecipient> pageResult = MATCH_ALL_SEARCH.equals(normalizedSearch)
                ? notificationRecipientRepository.findByUserId(user.getId(), pageable)
                : notificationRecipientRepository.findUserNotifications(user.getId(), normalizedSearch, pageable);

        return PageResponse.from(pageResult.map(this::toUserNotificationResponse));
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
        if (redisEnabled && pubSubEnabled && notificationRedisPublisher.publish(recipients)) {
            return;
        }

        List<NotificationPublishResult> results = notificationStreamService.publish(recipients);
        notificationDeliveryTrackingService.recordInAppResults(results);
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
