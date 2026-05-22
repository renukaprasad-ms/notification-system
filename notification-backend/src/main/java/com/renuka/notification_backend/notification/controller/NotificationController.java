package com.renuka.notification_backend.notification.controller;

import com.renuka.notification_backend.common.response.ApiResponse;
import com.renuka.notification_backend.notification.dto.SendAllNotificationRequest;
import com.renuka.notification_backend.notification.dto.SendNotificationResponse;
import com.renuka.notification_backend.notification.dto.SendSelectedNotificationRequest;
import com.renuka.notification_backend.notification.dto.UnreadCountResponse;
import com.renuka.notification_backend.notification.dto.UserNotificationResponse;
import com.renuka.notification_backend.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SendNotificationResponse>> sendToAllUsers(
            @Valid @RequestBody SendAllNotificationRequest request,
            Authentication authentication
    ) {
        SendNotificationResponse response = notificationService.sendToAllUsers(request, authentication.getName());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), response, "Notification sent successfully"));
    }

    @PostMapping("/send-selected")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SendNotificationResponse>> sendToSelectedUsers(
            @Valid @RequestBody SendSelectedNotificationRequest request,
            Authentication authentication
    ) {
        SendNotificationResponse response = notificationService.sendToSelectedUsers(request, authentication.getName());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), response, "Notification sent successfully"));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(Authentication authentication) {
        return notificationService.streamNotifications(authentication.getName());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<UserNotificationResponse>>> getMyNotifications(Authentication authentication) {
        List<UserNotificationResponse> response = notificationService.getMyNotifications(authentication.getName());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), response, "Notifications fetched successfully"));
    }

    @PatchMapping("/{recipientId}/viewed")
    public ResponseEntity<ApiResponse<UserNotificationResponse>> markViewed(
            @PathVariable UUID recipientId,
            Authentication authentication
    ) {
        UserNotificationResponse response = notificationService.markViewed(recipientId, authentication.getName());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), response, "Notification marked as viewed"));
    }

    @PatchMapping("/{recipientId}/read")
    public ResponseEntity<ApiResponse<UserNotificationResponse>> markRead(
            @PathVariable UUID recipientId,
            Authentication authentication
    ) {
        UserNotificationResponse response = notificationService.markRead(recipientId, authentication.getName());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), response, "Notification marked as read"));
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(Authentication authentication) {
        UnreadCountResponse response = notificationService.getUnreadCount(authentication.getName());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), response, "Unread count fetched successfully"));
    }
}
