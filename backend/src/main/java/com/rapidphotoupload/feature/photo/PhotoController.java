package com.rapidphotoupload.feature.photo;

import com.rapidphotoupload.application.command.UpdatePhotoMetadataCommand;
import com.rapidphotoupload.application.dto.PhotoResponse;
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
    
    /**
     * Get all photos for the authenticated user.
     * GET /api/v1/photos
     * Optional query param: sessionId
     */
    @GetMapping
    public ResponseEntity<List<PhotoResponse>> getPhotos(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID sessionId) {
        
        log.debug("Getting photos for user {}", principal.getUserId());
        
        GetPhotosQuery query = new GetPhotosQuery(
            principal.getUserId(),
            sessionId
        );
        
        List<PhotoResponse> photos = getPhotosQueryHandler.handle(query);
        
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
     * Request DTO for updating metadata.
     */
    public record UpdateMetadataRequest(List<String> tags) {}
}
