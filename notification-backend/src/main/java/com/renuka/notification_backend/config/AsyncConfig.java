package com.renuka.notification_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "notificationDispatchExecutor")
    public Executor notificationDispatchExecutor(
            @Value("${app.notification.dispatch.core-pool-size:4}") int corePoolSize,
            @Value("${app.notification.dispatch.max-pool-size:8}") int maxPoolSize,
            @Value("${app.notification.dispatch.queue-capacity:100}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("notification-dispatch-");
        executor.initialize();
        return executor;
    }
}
