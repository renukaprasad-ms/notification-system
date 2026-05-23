package com.renuka.notification_backend.notification.realtime;

import com.renuka.notification_backend.notification.entity.NotificationRecipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationRedisPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationRedisPublisher.class);

    private final RedisTemplate<String, NotificationRedisMessage> notificationRedisTemplate;
    private final ChannelTopic notificationChannelTopic;
    private final boolean redisEnabled;
    private final boolean pubSubEnabled;

    public NotificationRedisPublisher(
            RedisTemplate<String, NotificationRedisMessage> notificationRedisTemplate,
            ChannelTopic notificationChannelTopic,
            @Value("${app.redis.enabled:true}") boolean redisEnabled,
            @Value("${app.redis.pubsub.enabled:true}") boolean pubSubEnabled
    ) {
        this.notificationRedisTemplate = notificationRedisTemplate;
        this.notificationChannelTopic = notificationChannelTopic;
        this.redisEnabled = redisEnabled;
        this.pubSubEnabled = pubSubEnabled;
    }

    public boolean publish(List<NotificationRecipient> recipients) {
        if (!redisEnabled || !pubSubEnabled) {
            return false;
        }

        try {
            recipients.stream()
                    .map(NotificationRedisMessage::fromRecipient)
                    .forEach(message -> notificationRedisTemplate.convertAndSend(notificationChannelTopic.getTopic(), message));
            return true;
        } catch (Exception exception) {
            log.warn("Redis pub/sub unavailable. Falling back to local notification publish.", exception);
            return false;
        }
    }
}
