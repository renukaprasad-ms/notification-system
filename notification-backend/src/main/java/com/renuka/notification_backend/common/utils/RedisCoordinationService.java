package com.renuka.notification_backend.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisCoordinationService {

    private static final Logger log = LoggerFactory.getLogger(RedisCoordinationService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final boolean redisEnabled;

    public RedisCoordinationService(
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.redis.enabled:true}") boolean redisEnabled
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisEnabled = redisEnabled;
    }

    public boolean acquireLock(String key, Duration ttl) {
        if (!redisEnabled) {
            return true;
        }

        try {
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception exception) {
            log.warn("Redis lock unavailable for key {}", key, exception);
            return true;
        }
    }

    public void releaseLock(String key) {
        if (!redisEnabled) {
            return;
        }

        try {
            stringRedisTemplate.delete(key);
        } catch (Exception exception) {
            log.warn("Failed to release Redis lock for key {}", key, exception);
        }
    }
}
