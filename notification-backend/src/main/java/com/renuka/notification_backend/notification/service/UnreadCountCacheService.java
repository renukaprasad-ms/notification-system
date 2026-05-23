package com.renuka.notification_backend.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.LongSupplier;

@Service
public class UnreadCountCacheService {

    private static final Logger log = LoggerFactory.getLogger(UnreadCountCacheService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final boolean redisEnabled;
    private final Duration unreadCountTtl;

    public UnreadCountCacheService(
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.redis.enabled:true}") boolean redisEnabled,
            @Value("${app.redis.cache.unread-count-ttl-minutes:60}") long unreadCountTtlMinutes
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisEnabled = redisEnabled;
        this.unreadCountTtl = Duration.ofMinutes(unreadCountTtlMinutes);
    }

    public long getUnreadCount(UUID userId, LongSupplier databaseFallback) {
        if (!redisEnabled) {
            return databaseFallback.getAsLong();
        }

        String key = unreadCountKey(userId);

        try {
            String cachedValue = stringRedisTemplate.opsForValue().get(key);
            if (cachedValue != null) {
                return Long.parseLong(cachedValue);
            }

            long unreadCount = databaseFallback.getAsLong();
            stringRedisTemplate.opsForValue().set(key, Long.toString(unreadCount), unreadCountTtl);
            return unreadCount;
        } catch (Exception exception) {
            log.warn("Falling back to database unread count for user {}", userId, exception);
            return databaseFallback.getAsLong();
        }
    }

    public void incrementUnreadCounts(List<UUID> userIds) {
        if (!redisEnabled) {
            return;
        }

        userIds.forEach(this::incrementUnreadCount);
    }

    public void incrementUnreadCount(UUID userId) {
        if (!redisEnabled) {
            return;
        }

        try {
            Long updatedValue = stringRedisTemplate.opsForValue().increment(unreadCountKey(userId));
            if (updatedValue != null) {
                stringRedisTemplate.expire(unreadCountKey(userId), unreadCountTtl);
            }
        } catch (Exception exception) {
            log.warn("Failed to increment unread count cache for user {}", userId, exception);
        }
    }

    public void decrementUnreadCount(UUID userId) {
        if (!redisEnabled) {
            return;
        }

        try {
            Long updatedValue = stringRedisTemplate.opsForValue().decrement(unreadCountKey(userId));
            if (updatedValue != null && updatedValue < 0) {
                stringRedisTemplate.opsForValue().set(unreadCountKey(userId), "0", unreadCountTtl);
            } else {
                stringRedisTemplate.expire(unreadCountKey(userId), unreadCountTtl);
            }
        } catch (Exception exception) {
            log.warn("Failed to decrement unread count cache for user {}", userId, exception);
        }
    }

    public void evictUnreadCount(UUID userId) {
        if (!redisEnabled) {
            return;
        }

        try {
            stringRedisTemplate.delete(unreadCountKey(userId));
        } catch (Exception exception) {
            log.warn("Failed to evict unread count cache for user {}", userId, exception);
        }
    }

    public void evictAllUnreadCounts() {
        if (!redisEnabled) {
            return;
        }

        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match("notification:unread:*")
                .count(1000)
                .build();

        try (Cursor<String> keys = stringRedisTemplate.scan(scanOptions)) {
            keys.forEachRemaining(stringRedisTemplate::delete);
        } catch (Exception exception) {
            log.warn("Failed to evict unread count caches", exception);
        }
    }

    private String unreadCountKey(UUID userId) {
        return "notification:unread:" + userId;
    }
}
