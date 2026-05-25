package com.renuka.notification_backend.notification.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationQueueService {

    private static final Logger log = LoggerFactory.getLogger(NotificationQueueService.class);
    private static final String RECIPIENT_ID_FIELD = "recipientId";
    private static final String BOOTSTRAP_FIELD = "bootstrap";

    private final StringRedisTemplate stringRedisTemplate;
    private final boolean redisEnabled;
    private final boolean queueEnabled;
    private final String streamKey;
    private final String consumerGroup;

    public NotificationQueueService(
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.redis.enabled:true}") boolean redisEnabled,
            @Value("${app.notification.queue.enabled:true}") boolean queueEnabled,
            @Value("${app.notification.queue.stream-key:notification:dispatch}") String streamKey,
            @Value("${app.notification.queue.consumer-group:notification-dispatchers}") String consumerGroup
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisEnabled = redisEnabled;
        this.queueEnabled = queueEnabled;
        this.streamKey = streamKey;
        this.consumerGroup = consumerGroup;
    }

    @PostConstruct
    void ensureConsumerGroup() {
        if (!isEnabled()) {
            return;
        }

        try {
            RecordId bootstrapRecordId = stringRedisTemplate.opsForStream()
                    .add(MapRecord.create(streamKey, Map.of(BOOTSTRAP_FIELD, "1")));
            stringRedisTemplate.opsForStream().createGroup(streamKey, ReadOffset.latest(), consumerGroup);

            if (bootstrapRecordId != null) {
                stringRedisTemplate.opsForStream().delete(streamKey, bootstrapRecordId);
            }
        } catch (Exception exception) {
            if (!isBusyGroupError(exception)) {
                log.warn("Failed to initialize notification queue consumer group {}", consumerGroup, exception);
            }
        }
    }

    public boolean enqueueRecipientIds(List<UUID> recipientIds) {
        if (!isEnabled()) {
            return false;
        }

        if (recipientIds == null || recipientIds.isEmpty()) {
            return true;
        }

        try {
            recipientIds.forEach(recipientId -> stringRedisTemplate.opsForStream()
                    .add(MapRecord.create(streamKey, Map.of(RECIPIENT_ID_FIELD, recipientId.toString()))));
            return true;
        } catch (Exception exception) {
            log.warn("Failed to enqueue {} notification recipient(s)", recipientIds.size(), exception);
            return false;
        }
    }

    public List<MapRecord<String, Object, Object>> readBatch(String consumerName, int batchSize, Duration blockTimeout) {
        if (!isEnabled()) {
            return List.of();
        }

        try {
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                    Consumer.from(consumerGroup, consumerName),
                    StreamReadOptions.empty()
                            .count(batchSize)
                            .block(blockTimeout),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed())
            );
            return records == null ? List.of() : records;
        } catch (Exception exception) {
            log.warn("Failed to read notification queue batch for consumer {}", consumerName, exception);
            return List.of();
        }
    }

    public void acknowledgeAndDelete(RecordId recordId) {
        if (!isEnabled() || recordId == null) {
            return;
        }

        try {
            stringRedisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, recordId);
            stringRedisTemplate.opsForStream().delete(streamKey, recordId);
        } catch (Exception exception) {
            log.warn("Failed to acknowledge notification queue record {}", recordId.getValue(), exception);
        }
    }

    public boolean isEnabled() {
        return redisEnabled && queueEnabled;
    }

    private boolean isBusyGroupError(Exception exception) {
        return exception.getMessage() != null && exception.getMessage().contains("BUSYGROUP");
    }
}
