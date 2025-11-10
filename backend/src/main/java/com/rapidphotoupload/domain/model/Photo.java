package com.rapidphotoupload.domain.model;

import com.rapidphotoupload.domain.event.DomainEvent;
import com.rapidphotoupload.domain.event.PhotoUploadCompleted;
import com.rapidphotoupload.domain.event.PhotoUploadFailed;
import com.rapidphotoupload.domain.event.PhotoUploadInitiated;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class Photo {
    
    private UUID id;
    private UUID userId;
    private UUID uploadSessionId;
    
    // Direct fields for easy access
    private String s3Key;
    private String s3Bucket;
    private String s3Etag;
    private String originalFilename;
    private long fileSizeBytes;
    private String mimeType;
    private String thumbnailUrl;
    private String thumbnailFallbackUrl;
    private String placeholderUrl;
    private String placeholderFallbackUrl;
    private String placeholderBase64;
    private Instant createdAt;
    private Instant updatedAt;
    
    private PhotoMetadata metadata;
    private UploadStatus uploadStatus;
    
    private String multipartUploadId;
    private Instant uploadExpiresAt;
    
    private List<UploadPart> parts = new ArrayList<>();
    private List<DomainEvent> domainEvents = new ArrayList<>();
    
    private Long version;
    
    // Constructor used by InitiateUploadHandler
    public Photo(
        UUID id,
        UUID userId,
        String s3Key,
        String s3Bucket,
        String originalFilename,
        long fileSizeBytes,
        String mimeType
    ) {
        this.id = id;
        this.userId = userId;
        this.s3Key = s3Key;
        this.s3Bucket = s3Bucket;
        this.originalFilename = originalFilename;
        this.fileSizeBytes = fileSizeBytes;
        this.mimeType = mimeType;
        this.uploadStatus = UploadStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.version = null;
        
        this.metadata = new PhotoMetadata(new ArrayList<>());
    }
    
    // Full constructor used by PhotoRepositoryImpl
    public Photo(
        UUID id,
        UUID userId,
        UUID uploadSessionId,
        String s3Key,
        String s3Bucket,
        String s3Etag,
        String originalFilename,
        long fileSizeBytes,
        String mimeType,
        UploadStatus uploadStatus,
        String multipartUploadId,
        Instant uploadExpiresAt,
        PhotoMetadata metadata,
        String thumbnailUrl,
        String thumbnailFallbackUrl,
        String placeholderUrl,
        String placeholderFallbackUrl,
        String placeholderBase64,
        Instant createdAt,
        Instant updatedAt,
        Long version
    ) {
        this.id = id;
        this.userId = userId;
        this.uploadSessionId = uploadSessionId;
        this.s3Key = s3Key;
        this.s3Bucket = s3Bucket;
        this.s3Etag = s3Etag;
        this.originalFilename = originalFilename;
        this.fileSizeBytes = fileSizeBytes;
        this.mimeType = mimeType;
        this.uploadStatus = uploadStatus;
        this.multipartUploadId = multipartUploadId;
        this.uploadExpiresAt = uploadExpiresAt;
        this.metadata = metadata;
        this.thumbnailUrl = thumbnailUrl;
        this.thumbnailFallbackUrl = thumbnailFallbackUrl;
        this.placeholderUrl = placeholderUrl;
        this.placeholderFallbackUrl = placeholderFallbackUrl;
        this.placeholderBase64 = placeholderBase64;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }
    
    public void initiateUpload(String multipartUploadId) {
        if (this.uploadStatus != UploadStatus.PENDING) {
            throw new IllegalStateException("Upload already initiated");
        }
        
        this.multipartUploadId = multipartUploadId;
        this.uploadStatus = UploadStatus.INITIATED;
        this.uploadExpiresAt = Instant.now().plusSeconds(7200);
        this.updatedAt = Instant.now();
        
        addDomainEvent(new PhotoUploadInitiated(
            this.id,
            this.userId,
            this.originalFilename,
            Instant.now()
        ));
    }
    
    public void markPartUploaded(int partNumber, String etag, long sizeBytes) {
        if (this.uploadStatus != UploadStatus.INITIATED
            && this.uploadStatus != UploadStatus.UPLOADING) {
            throw new IllegalStateException("Cannot mark part uploaded in status: " + this.uploadStatus);
        }
        
        boolean partExists = this.parts.stream()
            .anyMatch(p -> p.getPartNumber() == partNumber);
        
        if (partExists) {
            throw new IllegalArgumentException("Part " + partNumber + " already uploaded");
        }
        
        UploadPart part = new UploadPart(UUID.randomUUID(), this.id, partNumber, etag, sizeBytes);
        this.parts.add(part);
        
        if (this.uploadStatus == UploadStatus.INITIATED) {
            this.uploadStatus = UploadStatus.UPLOADING;
        }
    }
    
    public void completeUpload(String finalEtag) {
        if (this.uploadStatus != UploadStatus.UPLOADING
            && this.uploadStatus != UploadStatus.INITIATED) {
            throw new IllegalStateException("Cannot complete upload in status: " + this.uploadStatus);
        }
        
        this.s3Etag = finalEtag;
        this.uploadStatus = UploadStatus.COMPLETED;
        this.uploadExpiresAt = null;
        this.updatedAt = Instant.now();
        
        addDomainEvent(new PhotoUploadCompleted(
            this.id,
            this.userId,
            this.s3Key,
            this.fileSizeBytes,
            Instant.now()
        ));
    }
    
    public void failUpload(String reason) {
        if (this.uploadStatus == UploadStatus.COMPLETED) {
            throw new IllegalStateException("Cannot fail completed upload");
        }
        
        UploadStatus previousStatus = this.uploadStatus;
        this.uploadStatus = UploadStatus.FAILED;
        this.uploadExpiresAt = null;
        
        addDomainEvent(new PhotoUploadFailed(
            this.id,
            this.userId,
            reason,
            previousStatus,
            Instant.now()
        ));
    }
    
    public void associateWithSession(UUID sessionId) {
        if (this.uploadSessionId != null) {
            throw new IllegalStateException("Photo already associated with session");
        }
        this.uploadSessionId = sessionId;
    }
    
    public void addTag(String tag) {
        this.metadata.addTag(tag);
        this.updatedAt = Instant.now();
    }
    
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
        this.updatedAt = Instant.now();
    }

    public void setThumbnailFallbackUrl(String thumbnailFallbackUrl) {
        this.thumbnailFallbackUrl = thumbnailFallbackUrl;
        this.updatedAt = Instant.now();
    }

    public void setPlaceholderUrl(String placeholderUrl) {
        this.placeholderUrl = placeholderUrl;
        this.updatedAt = Instant.now();
    }

    public void setPlaceholderFallbackUrl(String placeholderFallbackUrl) {
        this.placeholderFallbackUrl = placeholderFallbackUrl;
        this.updatedAt = Instant.now();
    }

    public void setPlaceholderBase64(String placeholderBase64) {
        this.placeholderBase64 = placeholderBase64;
        this.updatedAt = Instant.now();
    }
    
    public void setUploadExpiresAt(Instant expiresAt) {
        this.uploadExpiresAt = expiresAt;
    }
    
    private void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }
    
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
    
    public boolean isExpired() {
        return uploadExpiresAt != null && Instant.now().isAfter(uploadExpiresAt);
    }

}
