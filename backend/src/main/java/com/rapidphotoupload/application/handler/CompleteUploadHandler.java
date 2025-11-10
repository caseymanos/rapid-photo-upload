package com.rapidphotoupload.application.handler;

import com.rapidphotoupload.application.command.CompleteUploadCommand;
import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.domain.model.UploadSession;
import com.rapidphotoupload.domain.event.DomainEvent;
import com.rapidphotoupload.domain.repository.PhotoRepository;
import com.rapidphotoupload.domain.repository.UploadSessionRepository;
import com.rapidphotoupload.infrastructure.event.UploadNotificationService;
import com.rapidphotoupload.infrastructure.storage.S3StorageService;
import com.rapidphotoupload.infrastructure.media.PhotoDerivativeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Handler for completing photo uploads.
 * Completes multipart upload in S3 and updates photo status.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompleteUploadHandler {
    
      private final PhotoRepository photoRepository;
      private final UploadSessionRepository uploadSessionRepository;
      private final S3StorageService s3StorageService;
      private final UploadNotificationService notificationService;
      private final PhotoDerivativeService photoDerivativeService;
    
    @Transactional
    public void handle(CompleteUploadCommand command) {
        long startTime = System.currentTimeMillis();
        log.info("Completing upload for photo {}", command.getPhotoId());
        
        long loadStart = System.currentTimeMillis();
        Photo photo = photoRepository.findById(command.getPhotoId())
            .orElseThrow(() -> new IllegalArgumentException("Photo not found"));
        log.debug("Loaded metadata for photo {} in {}ms", command.getPhotoId(), System.currentTimeMillis() - loadStart);
        
        // Idempotency check - if already completed, return success
        if (photo.getUploadStatus() == com.rapidphotoupload.domain.model.UploadStatus.COMPLETED) {
            log.info("Upload already completed for photo {}, skipping duplicate request", command.getPhotoId());
            return;
        }
        
        // Verify ownership
        if (!photo.getUserId().equals(command.getUserId())) {
            throw new SecurityException("User does not own this photo");
        }
        
        try {
            // Mark parts as uploaded
            command.getParts().forEach(part ->
                photo.markPartUploaded(part.getPartNumber(), part.getEtag(), part.getSizeBytes())
            );
            
            boolean multipartUpload = photo.getMultipartUploadId() != null;

            String etag;
            if (multipartUpload) {
                long s3Start = System.currentTimeMillis();
                etag = s3StorageService.completeMultipartUpload(
                    photo.getS3Key(),
                    photo.getMultipartUploadId(),
                    command.getParts().stream()
                        .map(p -> new S3StorageService.CompletedPart(p.getPartNumber(), p.getEtag()))
                        .toList()
                );
                log.info("S3 completeMultipartUpload finished for photo {} in {}ms", command.getPhotoId(), System.currentTimeMillis() - s3Start);
            } else {
                if (command.getParts().isEmpty()) {
                    throw new IllegalStateException("At least one part is required to finalize upload");
                }
                etag = command.getParts().get(0).getEtag();
            }
            
            // Mark photo as completed
            photo.completeUpload(etag);
            
            // Update session if exists
            if (photo.getUploadSessionId() != null) {
                long sessionUpdateStart = System.currentTimeMillis();
                UploadSession session = uploadSessionRepository.findById(photo.getUploadSessionId())
                    .orElseThrow(() -> new IllegalArgumentException("Upload session not found"));

                session.markPhotoCompleted();
                uploadSessionRepository.save(session);
                log.debug("Updated upload session {} in {}ms", session.getId(), System.currentTimeMillis() - sessionUpdateStart);
            }
            
            // Capture events to publish asynchronously after transaction commits
            List<DomainEvent> events = List.copyOf(photo.getDomainEvents());
            UUID userId = photo.getUserId();
            UUID photoId = photo.getId();
            photo.clearDomainEvents();

            photoRepository.save(photo);

            // Fire-and-forget thumbnail generation
            photoDerivativeService.generateDerivativesAsync(photo.getId());

            // Schedule async notifications after transaction commits successfully
            notificationService.publishEvents(events);
            notificationService.notifyProgress(userId, photoId, 100, "COMPLETED");

            long duration = System.currentTimeMillis() - startTime;
            log.info("Upload completed successfully for photo {} in {}ms", command.getPhotoId(), duration);
            
        } catch (Exception e) {
            log.error("Failed to complete upload for photo {}", command.getPhotoId(), e);
            
            // Mark as failed
            photo.failUpload(e.getMessage());

            // Update session
            if (photo.getUploadSessionId() != null) {
                uploadSessionRepository.findById(photo.getUploadSessionId())
                    .ifPresent(session -> {
                        session.markPhotoFailed();
                        uploadSessionRepository.save(session);
                    });
            }

            List<DomainEvent> events = List.copyOf(photo.getDomainEvents());
            photo.clearDomainEvents();
            photoRepository.save(photo);

            // Notify client of failure via WebSocket (sync for immediate feedback)
            notificationService.publishEvents(events);
            notificationService.notifyProgress(photo.getUserId(), photo.getId(), 0, "FAILED");

            throw new RuntimeException("Failed to complete upload", e);
        }
    }

}
