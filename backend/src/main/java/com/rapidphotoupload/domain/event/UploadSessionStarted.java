package com.rapidphotoupload.domain.event;

import java.time.Instant;
import java.util.UUID;

public record UploadSessionStarted(
    UUID sessionId,
    UUID userId,
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
        return "UploadSessionStarted";
    }
}