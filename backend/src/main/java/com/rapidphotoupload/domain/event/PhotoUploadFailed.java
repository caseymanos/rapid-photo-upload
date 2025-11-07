package com.rapidphotoupload.domain.event;

import com.rapidphotoupload.domain.model.UploadStatus;

import java.time.Instant;
import java.util.UUID;

public record PhotoUploadFailed(
    UUID photoId,
    UUID userId,
    String reason,
    UploadStatus previousStatus,
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
        return "PhotoUploadFailed";
    }
}