package com.rapidphotoupload.domain.event;

import java.time.Instant;
import java.util.UUID;

public record PhotoUploadInitiated(
    UUID photoId,
    UUID userId,
    String filename,
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
        return "PhotoUploadInitiated";
    }
}