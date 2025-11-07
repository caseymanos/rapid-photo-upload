package com.rapidphotoupload.application.handler;

import com.rapidphotoupload.application.command.CompleteUploadCommand;
import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.domain.model.UploadSession;
import com.rapidphotoupload.domain.repository.PhotoRepository;
import com.rapidphotoupload.domain.repository.UploadSessionRepository;
  import com.rapidphotoupload.infrastructure.event.DomainEventPublisher;
  import com.rapidphotoupload.infrastructure.event.UploadProgressPublisher;
  import com.rapidphotoupload.infrastructure.storage.S3StorageService;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

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
      private final DomainEventPublisher eventPublisher;
      private final UploadProgressPublisher progressPublisher;
    
    @Transactional
    public void handle(CompleteUploadCommand command) {
        log.info("Completing upload for photo {}", command.getPhotoId());
        
        // Retrieve photo
        Photo photo = photoRepository.findById(command.getPhotoId())
            .orElseThrow(() -> new IllegalArgumentException("Photo not found"));
        
        // Verify ownership
        if (!photo.getUserId().equals(command.getUserId())) {
            throw new SecurityException("User does not own this photo");
        }
        
        try {
            // Mark parts as uploaded
            command.getParts().forEach(part ->
                photo.markPartUploaded(part.getPartNumber(), part.getEtag(), part.getSizeBytes())
            );
            
            // Complete multipart upload in S3
            String etag = s3StorageService.completeMultipartUpload(
                photo.getS3Key(),
                photo.getMultipartUploadId(),
                command.getParts().stream()
                    .map(p -> new S3StorageService.CompletedPart(p.getPartNumber(), p.getEtag()))
                    .toList()
            );
            
            // Mark photo as completed
            photo.completeUpload(etag);
            
            // Update session if exists
            if (photo.getUploadSessionId() != null) {
                UploadSession session = uploadSessionRepository.findById(photo.getUploadSessionId())
                    .orElseThrow(() -> new IllegalArgumentException("Upload session not found"));
                
                session.markPhotoCompleted();
                uploadSessionRepository.save(session);
            }
            
              // Save photo
              Photo savedPhoto = photoRepository.save(photo);

              // Publish domain events
              eventPublisher.publishAll(savedPhoto.getDomainEvents());
              photo.clearDomainEvents();

              // Notify client of completion via WebSocket
              progressPublisher.notifyProgress(
                  savedPhoto.getUserId(),
                  savedPhoto.getId(),
                  100,
                  "COMPLETED"
              );

              log.info("Upload completed successfully for photo {}", command.getPhotoId());
            
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

            photoRepository.save(photo);

            // Notify client of failure via WebSocket
            progressPublisher.notifyProgress(
                photo.getUserId(),
                photo.getId(),
                0,
                "FAILED"
            );
            
            throw new RuntimeException("Failed to complete upload", e);
        }
    }
}
