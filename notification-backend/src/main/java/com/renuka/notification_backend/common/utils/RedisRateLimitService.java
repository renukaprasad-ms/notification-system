package com.renuka.notification_backend.common.utils;

import com.renuka.notification_backend.common.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final boolean redisEnabled;

    public RedisRateLimitService(
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.redis.enabled:true}") boolean redisEnabled
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisEnabled = redisEnabled;
    }

    public void assertAllowed(String namespace, String identifier, long limit, Duration window, String message) {
        if (!redisEnabled) {
            return;
        }

        String key = namespace + ":" + identifier;

        try {
            Long currentCount = stringRedisTemplate.opsForValue().increment(key);
            if (currentCount == null) {
                return;
            }

            if (currentCount == 1L) {
                stringRedisTemplate.expire(key, window);
            }

            if (currentCount > limit) {
                throw new TooManyRequestsException(message);
            }
        } catch (TooManyRequestsException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Redis rate limiting unavailable for key {}", key, exception);
        }
    }
}
