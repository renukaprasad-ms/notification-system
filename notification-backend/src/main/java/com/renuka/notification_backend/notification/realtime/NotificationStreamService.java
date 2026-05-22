package com.renuka.notification_backend.notification.realtime;

import com.renuka.notification_backend.notification.dto.NotificationEventResponse;
import com.renuka.notification_backend.notification.entity.Notification;
import com.renuka.notification_backend.notification.entity.NotificationRecipient;
import com.renuka.notification_backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationStreamService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<UUID, Map<String, SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(User user, String clientId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        UUID userId = user.getId();

        Map<String, SseEmitter> userEmitters = emittersByUserId.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>());
        SseEmitter previousEmitter = userEmitters.put(clientId, emitter);
        if (previousEmitter != null) {
            previousEmitter.complete();
        }

        emitter.onCompletion(() -> removeEmitter(userId, clientId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, clientId, emitter));
        emitter.onError(error -> removeEmitter(userId, clientId, emitter));

        sendToEmitter(userId, clientId, emitter, "connected", "connected");
        return emitter;
    }

    public List<NotificationPublishResult> publish(List<NotificationRecipient> recipients) {
        List<NotificationPublishResult> results = new ArrayList<>();
        recipients.forEach(recipient -> results.add(publish(recipient)));
        return results;
    }

    private NotificationPublishResult publish(NotificationRecipient recipient) {
        Map<String, SseEmitter> emitters = emittersByUserId.get(recipient.getUser().getId());
        if (emitters == null || emitters.isEmpty()) {
            return NotificationPublishResult.failed(recipient.getId(), "No active SSE connection");
        }

        NotificationEventResponse event = toEvent(recipient);
        int deliveredCount = 0;
        String lastErrorMessage = null;

        for (Map.Entry<String, SseEmitter> emitterEntry : emitters.entrySet()) {
            boolean delivered = sendToEmitter(
                    recipient.getUser().getId(),
                    emitterEntry.getKey(),
                    emitterEntry.getValue(),
                    "notification",
                    event
            );
            if (delivered) {
                deliveredCount++;
            } else {
                lastErrorMessage = "Failed to send SSE event";
            }
        }

        if (deliveredCount > 0) {
            return NotificationPublishResult.delivered(recipient.getId());
        }

        return NotificationPublishResult.failed(recipient.getId(), lastErrorMessage);
    }

    private NotificationEventResponse toEvent(NotificationRecipient recipient) {
        Notification notification = recipient.getNotification();
        return new NotificationEventResponse(
                recipient.getId(),
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getPriority(),
                notification.getCreatedAt()
        );
    }

    private boolean sendToEmitter(UUID userId, String clientId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
            return true;
        } catch (IOException | IllegalStateException exception) {
            removeEmitter(userId, clientId, emitter);
            return false;
        }
    }

    private void removeEmitter(UUID userId, String clientId, SseEmitter emitter) {
        Map<String, SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null) {
            return;
        }

        emitters.computeIfPresent(clientId, (ignored, currentEmitter) -> currentEmitter == emitter ? null : currentEmitter);
        if (emitters.isEmpty()) {
            emittersByUserId.remove(userId);
        }
    }
}
