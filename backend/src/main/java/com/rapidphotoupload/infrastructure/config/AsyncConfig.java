package com.rapidphotoupload.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for async processing.
 * Configures thread pool for handling concurrent uploads (up to 100).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "uploadExecutor")
    public Executor uploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size: 50 threads always available
        executor.setCorePoolSize(50);
        
        // Max pool size: 100 concurrent uploads (as per requirements)
        executor.setMaxPoolSize(100);
        
        // Queue capacity: additional 100 uploads can wait
        executor.setQueueCapacity(100);
        
        // Thread naming
        executor.setThreadNamePrefix("upload-async-");
        
        // Rejection policy: caller runs (backpressure)
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }

    @Bean(name = "thumbnailExecutor")
    public Executor thumbnailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("thumbnail-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}
