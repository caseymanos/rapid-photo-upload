package com.rapidphotoupload.application.handler;

import com.rapidphotoupload.application.dto.PhotoResponse;
import com.rapidphotoupload.application.query.GetPhotoByIdQuery;
import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.domain.repository.PhotoRepository;
import com.rapidphotoupload.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for querying a single photo.
 * Retrieves photo details by ID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetPhotoByIdQueryHandler {

    private final PhotoRepository photoRepository;
    private final S3StorageService s3StorageService;
    
    @Transactional(readOnly = true)
    public PhotoResponse handle(GetPhotoByIdQuery query) {
        log.debug("Querying photo {}", query.getPhotoId());
        
        Photo photo = photoRepository.findById(query.getPhotoId())
            .orElseThrow(() -> new IllegalArgumentException("Photo not found"));
        
        // Verify ownership
        if (!photo.getUserId().equals(query.getUserId())) {
            throw new SecurityException("User does not own this photo");
        }
        
        return toResponse(photo);
    }
    
    private PhotoResponse toResponse(Photo photo) {
        String downloadUrl = null;
        try {
            downloadUrl = s3StorageService.generatePresignedDownloadUrl(photo.getS3Key());
        } catch (Exception e) {
            log.error("Failed to generate download URL for photo {}", photo.getId(), e);
        }

        return new PhotoResponse(
            photo.getId(),
            photo.getUserId(),
            photo.getUploadSessionId(),
            photo.getS3Key(),
            photo.getS3Bucket(),
            photo.getOriginalFilename(),
            photo.getFileSizeBytes(),
            photo.getMimeType(),
            photo.getUploadStatus().name(),
            photo.getMetadata().getTags(),
            photo.getThumbnailUrl(),
            photo.getThumbnailFallbackUrl(),
            photo.getPlaceholderUrl(),
            photo.getPlaceholderFallbackUrl(),
            photo.getPlaceholderBase64(),
            downloadUrl,
            photo.getCreatedAt(),
            photo.getUpdatedAt()
        );
    }
}
