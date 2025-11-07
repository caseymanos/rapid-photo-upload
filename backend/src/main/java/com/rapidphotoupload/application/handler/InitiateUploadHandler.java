package com.rapidphotoupload.application.handler;

import com.rapidphotoupload.application.command.InitiateUploadCommand;
import com.rapidphotoupload.application.dto.InitiateUploadResponse;
import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.domain.model.UploadSession;
import com.rapidphotoupload.domain.repository.PhotoRepository;
import com.rapidphotoupload.domain.repository.UploadSessionRepository;
import com.rapidphotoupload.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Handler for initiating photo uploads.
 * Creates photo entity and generates presigned URLs for S3 upload.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InitiateUploadHandler {
    
    private final PhotoRepository photoRepository;
    private final UploadSessionRepository uploadSessionRepository;
    private final S3StorageService s3StorageService;
    
    private static final long PART_SIZE = 5 * 1024 * 1024; // 5MB chunks
    
    @Transactional
    public InitiateUploadResponse handle(InitiateUploadCommand command) {
        log.info("Initiating upload for user {} - file: {}", command.getUserId(), command.getOriginalFilename());
        
        // Create photo entity
        Photo photo = new Photo(
            UUID.randomUUID(),
            command.getUserId(),
            generateS3Key(command.getUserId(), command.getOriginalFilename()),
            s3StorageService.getBucketName(),
            command.getOriginalFilename(),
            command.getFileSizeBytes(),
            command.getMimeType()
        );
        
        // Associate with session if provided
        if (command.getUploadSessionId() != null) {
            UploadSession session = uploadSessionRepository.findById(command.getUploadSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Upload session not found"));
            
            photo.associateWithSession(session.getId());
            session.registerPhoto();
            uploadSessionRepository.save(session);
        }
        
        // Add tags
        if (command.getTags() != null) {
            command.getTags().forEach(photo::addTag);
        }
        
        // Calculate number of parts needed
        int totalParts = (int) Math.ceil((double) command.getFileSizeBytes() / PART_SIZE);
        
        // Initiate multipart upload in S3
        String multipartUploadId = s3StorageService.initiateMultipartUpload(
            photo.getS3Key(),
            command.getMimeType()
        );
        
        photo.initiateUpload(multipartUploadId);
        
        // Set expiration (2 hours)
        Instant expiresAt = Instant.now().plus(2, ChronoUnit.HOURS);
        photo.setUploadExpiresAt(expiresAt);
        
        // Save photo
        Photo savedPhoto = photoRepository.save(photo);
        
        // Generate presigned URLs for each part
        List<InitiateUploadResponse.PresignedPartUrl> presignedUrls = 
            s3StorageService.generatePresignedUploadUrls(
                photo.getS3Key(),
                multipartUploadId,
                totalParts
            );
        
        log.info("Upload initiated for photo {} with {} parts", savedPhoto.getId(), totalParts);
        
        return new InitiateUploadResponse(
            savedPhoto.getId(),
            savedPhoto.getS3Key(),
            multipartUploadId,
            presignedUrls,
            expiresAt
        );
    }
    
    private String generateS3Key(UUID userId, String filename) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String sanitizedFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("uploads/%s/%s-%s", userId, timestamp, sanitizedFilename);
    }
}
