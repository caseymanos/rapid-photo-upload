package com.rapidphotoupload.feature.photo;

import com.rapidphotoupload.application.command.DeletePhotoCommand;
import com.rapidphotoupload.application.command.DeletePhotosCommand;
import com.rapidphotoupload.application.command.UpdatePhotoMetadataCommand;
import com.rapidphotoupload.application.dto.PhotoPageResponse;
import com.rapidphotoupload.application.dto.PhotoResponse;
import com.rapidphotoupload.application.handler.DeletePhotoHandler;
import com.rapidphotoupload.application.handler.GetPhotoByIdQueryHandler;
import com.rapidphotoupload.application.handler.GetPhotosQueryHandler;
import com.rapidphotoupload.application.handler.UpdatePhotoMetadataHandler;
import com.rapidphotoupload.application.query.GetPhotoByIdQuery;
import com.rapidphotoupload.application.query.GetPhotosQuery;
import com.rapidphotoupload.infrastructure.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for photo operations (Vertical Slice).
 * Handles photo retrieval, metadata updates, and gallery operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
public class PhotoController {
    
    private final GetPhotosQueryHandler getPhotosQueryHandler;
    private final GetPhotoByIdQueryHandler getPhotoByIdQueryHandler;
    private final UpdatePhotoMetadataHandler updatePhotoMetadataHandler;
    private final DeletePhotoHandler deletePhotoHandler;
    
    /**
     * Get all photos for the authenticated user.
     * GET /api/v1/photos
     * Optional query param: sessionId
     */
    @GetMapping
    public ResponseEntity<PhotoPageResponse> getPhotos(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "30") int size,
            @RequestParam(name = "includeDownloadUrl", defaultValue = "true") boolean includeDownloadUrl) {
        
        log.debug("Getting photos for user {} (page={}, size={})",
            principal.getUserId(), page, size);
        
        GetPhotosQuery query = new GetPhotosQuery(
            principal.getUserId(),
            sessionId,
            includeDownloadUrl,
            page,
            size
        );
        
        PhotoPageResponse photos = getPhotosQueryHandler.handle(query);
        
        return ResponseEntity.ok(photos);
    }
    
    /**
     * Get a specific photo by ID.
     * GET /api/v1/photos/{photoId}
     */
    @GetMapping("/{photoId}")
    public ResponseEntity<PhotoResponse> getPhoto(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID photoId) {
        
        log.debug("Getting photo {}", photoId);
        
        GetPhotoByIdQuery query = new GetPhotoByIdQuery(
            photoId,
            principal.getUserId()
        );
        
        PhotoResponse photo = getPhotoByIdQueryHandler.handle(query);
        
        return ResponseEntity.ok(photo);
    }

    /**
     * Get only the download URL for a photo.
     * GET /api/v1/photos/{photoId}/download-url
     */
    @GetMapping("/{photoId}/download-url")
    public ResponseEntity<DownloadUrlResponse> getPhotoDownloadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID photoId) {

        log.debug("Getting download URL for photo {}", photoId);

        GetPhotoByIdQuery query = new GetPhotoByIdQuery(
            photoId,
            principal.getUserId()
        );

        PhotoResponse photo = getPhotoByIdQueryHandler.handle(query);
        return ResponseEntity.ok(new DownloadUrlResponse(photo.getDownloadUrl()));
    }
    
    /**
     * Update photo metadata (tags).
     * PUT /api/v1/photos/{photoId}/metadata
     */
    @PutMapping("/{photoId}/metadata")
    public ResponseEntity<Void> updateMetadata(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID photoId,
            @RequestBody UpdateMetadataRequest request) {
        
        log.info("Updating metadata for photo {}", photoId);
        
        UpdatePhotoMetadataCommand command = new UpdatePhotoMetadataCommand(
            photoId,
            principal.getUserId(),
            request.tags()
        );
        
        updatePhotoMetadataHandler.handle(command);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete a specific photo by ID.
     */
    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID photoId) {
        log.info("Deleting photo {} for user {}", photoId, principal.getUserId());
        deletePhotoHandler.handle(new DeletePhotoCommand(photoId, principal.getUserId()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete a set of photos by ID.
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Void> deletePhotos(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody BatchDeleteRequest request) {
        log.info("Batch deleting {} photos for user {}", request.photoIds().size(), principal.getUserId());
        deletePhotoHandler.handle(new DeletePhotosCommand(principal.getUserId(), request.photoIds()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete all photos for the authenticated user.
     */
    @DeleteMapping
    public ResponseEntity<DeleteSummaryResponse> deleteAllPhotos(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Deleting all photos for user {}", principal.getUserId());
        int deletedCount = deletePhotoHandler.deleteAllForUser(principal.getUserId());
        return ResponseEntity.ok(new DeleteSummaryResponse(deletedCount));
    }
    
    /**
     * Request DTO for updating metadata.
     */
    public record UpdateMetadataRequest(List<String> tags) {}

    public record BatchDeleteRequest(List<UUID> photoIds) {}

    public record DeleteSummaryResponse(int deletedCount) {}

    public record DownloadUrlResponse(String downloadUrl) {}
}
