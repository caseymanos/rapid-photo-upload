package com.rapidphotoupload.application.handler;

import com.rapidphotoupload.application.dto.PhotoPageResponse;
import com.rapidphotoupload.application.dto.PhotoResponse;
import com.rapidphotoupload.application.mapper.PhotoResponseMapper;
import com.rapidphotoupload.application.query.GetPhotosQuery;
import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.domain.repository.PhotoRepository;
import com.rapidphotoupload.infrastructure.storage.S3StorageService;
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

    private static final int MAX_PAGE_SIZE = 100;

    private final PhotoRepository photoRepository;
    private final S3StorageService s3StorageService;
    private final PhotoResponseMapper photoResponseMapper;
    
    @Transactional(readOnly = true)
    public PhotoPageResponse handle(GetPhotosQuery query) {
        long startTime = System.currentTimeMillis();
        log.debug("Querying photos for user {} (page={}, size={})",
            query.getUserId(), query.getPage(), query.getSize());
        
        List<Photo> photos;
        
        if (query.getUploadSessionId() != null) {
            photos = photoRepository.findByUploadSessionId(query.getUploadSessionId());
        } else {
            photos = photoRepository.findByUserId(query.getUserId());
        }

        int total = photos.size();
        int page = Math.max(0, query.getPage());
        int size = Math.max(1, Math.min(query.getSize(), MAX_PAGE_SIZE));
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        boolean hasNext = toIndex < total;
        
        List<PhotoResponse> responses = photos.subList(fromIndex, toIndex).parallelStream()
            .map(photo -> toResponse(photo, query.isIncludeDownloadUrl()))
            .collect(Collectors.toList());
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Retrieved page {} ({} items, total={}, includeDownloadUrl={}) in {}ms",
            page, responses.size(), total, query.isIncludeDownloadUrl(), duration);
        
        return new PhotoPageResponse(responses, page, size, total, hasNext);
    }
    
    private PhotoResponse toResponse(Photo photo, boolean includeDownloadUrl) {
        String downloadUrl = null;
        if (includeDownloadUrl) {
            try {
                downloadUrl = s3StorageService.generatePresignedDownloadUrl(photo.getS3Key());
            } catch (Exception e) {
                log.error("Failed to generate download URL for photo {}", photo.getId(), e);
            }
        }

        return photoResponseMapper.toResponse(photo, downloadUrl);
    }
}
