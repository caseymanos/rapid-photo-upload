package com.rapidphotoupload.infrastructure.event;

import com.rapidphotoupload.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Dispatches upload-related notifications asynchronously so the main request thread
 * does not block on slow downstream integrations (EventBridge/WebSocket).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadNotificationService {

    private final DomainEventPublisher eventPublisher;
    private final UploadProgressPublisher progressPublisher;

    @Async("uploadExecutor")
    public void publishEvents(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        try {
            eventPublisher.publishAll(events);
        } catch (Exception e) {
            log.error("Failed to publish upload events asynchronously", e);
        }
    }

    @Async("uploadExecutor")
    public void notifyProgress(UUID userId, UUID photoId, int progress, String status) {
        try {
            progressPublisher.notifyProgress(userId, photoId, progress, status);
        } catch (Exception e) {
            log.error("Failed to notify upload progress asynchronously for photo {}", photoId, e);
        }
    }
}

