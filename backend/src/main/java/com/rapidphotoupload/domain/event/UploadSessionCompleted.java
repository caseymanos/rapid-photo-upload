package com.rapidphotoupload.domain.event;

import java.time.Instant;
import java.util.UUID;

public record UploadSessionCompleted(
    UUID sessionId,
    UUID userId,
    int totalPhotos,
    int completedPhotos,
    int failedPhotos,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID getAggregateId() {
        return sessionId;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "UploadSessionCompleted";
    }
}