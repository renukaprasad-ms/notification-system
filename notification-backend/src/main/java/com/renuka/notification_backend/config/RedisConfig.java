package com.renuka.notification_backend.config;

import com.renuka.notification_backend.notification.realtime.NotificationRedisMessage;
import com.renuka.notification_backend.notification.realtime.NotificationRedisSubscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, NotificationRedisMessage> notificationRedisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        JdkSerializationRedisSerializer serializer = new JdkSerializationRedisSerializer();
        RedisTemplate<String, NotificationRedisMessage> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public ChannelTopic notificationChannelTopic(
            @Value("${app.redis.pubsub.notification-channel:notification-events}") String channelName
    ) {
        return new ChannelTopic(channelName);
    }

    @Bean
    @ConditionalOnProperty(name = "app.redis.pubsub.enabled", havingValue = "true", matchIfMissing = true)
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter notificationMessageListenerAdapter,
            ChannelTopic notificationChannelTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(notificationMessageListenerAdapter, notificationChannelTopic);
        container.setErrorHandler(Throwable::printStackTrace);
        return container;
    }

    @Bean
    @ConditionalOnProperty(name = "app.redis.pubsub.enabled", havingValue = "true", matchIfMissing = true)
    public MessageListenerAdapter notificationMessageListenerAdapter(NotificationRedisSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "handleMessage");
        adapter.setSerializer(new JdkSerializationRedisSerializer());
        return adapter;
    }
}
