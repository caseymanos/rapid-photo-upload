package com.rapidphotoupload.domain.event;

import java.time.Instant;
import java.util.UUID;

public record PhotoUploadCompleted(
    UUID photoId,
    UUID userId,
    String s3Key,
    long fileSizeBytes,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID getAggregateId() {
        return photoId;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "PhotoUploadCompleted";
    }
}