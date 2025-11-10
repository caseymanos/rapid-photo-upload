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
    
    private static final long MIN_PART_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final long TARGET_PART_SIZE_BYTES = 8 * 1024 * 1024; // 8MB
    private static final long SIMPLE_UPLOAD_THRESHOLD_BYTES = 25 * 1024 * 1024; // 25MB
    private static final int MAX_PARTS = 10;
    
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
        
        boolean singlePartUpload = command.getFileSizeBytes() <= SIMPLE_UPLOAD_THRESHOLD_BYTES;
        long chunkSizeBytes = singlePartUpload
            ? Math.max(MIN_PART_SIZE_BYTES, command.getFileSizeBytes())
            : determineChunkSize(command.getFileSizeBytes());

        String multipartUploadId = null;
        List<InitiateUploadResponse.PresignedPartUrl> presignedUrls;

        if (singlePartUpload) {
            photo.initiateUpload(null);
            String putUrl = s3StorageService.generatePresignedPutObjectUrl(
                photo.getS3Key(),
                command.getMimeType()
            );
            presignedUrls = List.of(new InitiateUploadResponse.PresignedPartUrl(1, putUrl));
        } else {
            int totalParts = (int) Math.ceil((double) command.getFileSizeBytes() / chunkSizeBytes);

            multipartUploadId = s3StorageService.initiateMultipartUpload(
                photo.getS3Key(),
                command.getMimeType()
            );
            photo.initiateUpload(multipartUploadId);

            presignedUrls = s3StorageService.generatePresignedUploadUrls(
                photo.getS3Key(),
                multipartUploadId,
                totalParts
            );
        }

        // Set expiration (2 hours)
        Instant expiresAt = Instant.now().plus(2, ChronoUnit.HOURS);
        photo.setUploadExpiresAt(expiresAt);
        
        // Save photo
        Photo savedPhoto = photoRepository.save(photo);
        
        log.info("Upload initiated for photo {} with strategy {} (chunkSize={} bytes)", savedPhoto.getId(),
            singlePartUpload ? "SINGLE" : "MULTIPART", chunkSizeBytes);
        
        return new InitiateUploadResponse(
            savedPhoto.getId(),
            savedPhoto.getS3Key(),
            multipartUploadId,
            presignedUrls,
            expiresAt,
            singlePartUpload,
            chunkSizeBytes
        );
    }
    
    private String generateS3Key(UUID userId, String filename) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String sanitizedFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("uploads/%s/%s-%s", userId, timestamp, sanitizedFilename);
    }

    private long determineChunkSize(long fileSizeBytes) {
        long chunkSize = TARGET_PART_SIZE_BYTES;
        long estimatedParts = (long) Math.ceil((double) fileSizeBytes / chunkSize);
        if (estimatedParts > MAX_PARTS) {
            chunkSize = (long) Math.ceil((double) fileSizeBytes / MAX_PARTS);
        }
        return Math.max(MIN_PART_SIZE_BYTES, chunkSize);
    }
}
