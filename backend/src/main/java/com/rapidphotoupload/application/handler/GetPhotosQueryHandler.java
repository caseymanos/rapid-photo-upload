package com.rapidphotoupload.application.handler;

import com.rapidphotoupload.application.dto.PhotoResponse;
import com.rapidphotoupload.application.query.GetPhotosQuery;
import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.domain.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for querying photos.
 * Retrieves photos for a user, optionally filtered by session.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetPhotosQueryHandler {
    
    private final PhotoRepository photoRepository;
    
    @Transactional(readOnly = true)
    public List<PhotoResponse> handle(GetPhotosQuery query) {
        log.debug("Querying photos for user {}", query.getUserId());
        
        List<Photo> photos;
        
        if (query.getUploadSessionId() != null) {
            photos = photoRepository.findByUploadSessionId(query.getUploadSessionId());
        } else {
            photos = photoRepository.findByUserId(query.getUserId());
        }
        
        return photos.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    private PhotoResponse toResponse(Photo photo) {
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
            photo.getCreatedAt(),
            photo.getUpdatedAt()
        );
    }
}
