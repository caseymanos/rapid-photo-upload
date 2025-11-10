package com.rapidphotoupload.feature.upload;

import com.rapidphotoupload.application.command.CompleteUploadCommand;
import com.rapidphotoupload.application.command.CreateSessionCommand;
import com.rapidphotoupload.application.command.InitiateUploadCommand;
import com.rapidphotoupload.application.dto.*;
import com.rapidphotoupload.application.handler.CompleteUploadHandler;
import com.rapidphotoupload.application.handler.CreateSessionHandler;
import com.rapidphotoupload.application.handler.InitiateUploadHandler;
import com.rapidphotoupload.application.handler.GetUploadStatusQueryHandler;
import com.rapidphotoupload.application.handler.UpdateSessionMetricsHandler;
import com.rapidphotoupload.application.command.UpdateSessionMetricsCommand;
import com.rapidphotoupload.application.query.GetUploadStatusQuery;
import com.rapidphotoupload.domain.model.UploadSession;
import com.rapidphotoupload.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for upload operations (Vertical Slice).
 * Handles upload initiation, completion, and session management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {
    
    private final InitiateUploadHandler initiateUploadHandler;
    private final CompleteUploadHandler completeUploadHandler;
    private final CreateSessionHandler createSessionHandler;
    private final GetUploadStatusQueryHandler getUploadStatusQueryHandler;
    private final UpdateSessionMetricsHandler updateSessionMetricsHandler;
    
    /**
     * Create a new upload session.
     * POST /api/v1/uploads/sessions
     */
    @PostMapping("/sessions")
    public ResponseEntity<SessionStatusResponse> createSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateSessionRequest request) {
        
        log.info("Creating upload session for user {}", principal.getUserId());
        
        CreateSessionCommand command = new CreateSessionCommand(
            principal.getUserId(),
            request.getExpectedPhotoCount()
        );
        
        UploadSession session = createSessionHandler.handle(command);
        
        SessionStatusResponse response = new SessionStatusResponse(
            session.getId(),
            session.getSessionToken(),
            session.getStatus().name(),
            session.getTotalPhotos(),
            session.getCompletedPhotos(),
            session.getFailedPhotos(),
            session.getStartedAt(),
            session.getCompletedAt()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Initiate a photo upload.
     * POST /api/v1/uploads/initiate
     */
    @PostMapping("/initiate")
    public ResponseEntity<InitiateUploadResponse> initiateUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InitiateUploadRequest request) {
        
        log.info("Initiating upload for user {} - file: {}", 
            principal.getUserId(), request.getOriginalFilename());
        
        InitiateUploadCommand command = new InitiateUploadCommand(
            principal.getUserId(),
            request.getOriginalFilename(),
            request.getMimeType(),
            request.getFileSizeBytes(),
            request.getUploadSessionId(),
            request.getTags()
        );
        
        InitiateUploadResponse response = initiateUploadHandler.handle(command);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Complete a photo upload.
     * POST /api/v1/uploads/{photoId}/complete
     */
    @PostMapping("/{photoId}/complete")
    public ResponseEntity<Void> completeUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID photoId,
            @Valid @RequestBody CompleteUploadRequest request) {
        
        log.info("Completing upload for photo {}", photoId);
        
        CompleteUploadCommand command = new CompleteUploadCommand(
            photoId,
            principal.getUserId(),
            request.getParts().stream()
                .map(p -> new CompleteUploadCommand.PartInfo(
                    p.getPartNumber(),
                    p.getEtag(),
                    p.getSizeBytes()
                ))
                .collect(Collectors.toList())
        );
        
        completeUploadHandler.handle(command);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get upload session status.
     * GET /api/v1/uploads/sessions/{sessionId}
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionStatusResponse> getSessionStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        
        log.debug("Getting session status for {}", sessionId);
        
        GetUploadStatusQuery query = new GetUploadStatusQuery(
            sessionId,
            principal.getUserId()
        );
        
        SessionStatusResponse response = getUploadStatusQueryHandler.handle(query);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update performance metrics for a session.
     * POST /api/v1/uploads/sessions/{sessionId}/metrics
     */
    @PostMapping("/sessions/{sessionId}/metrics")
    public ResponseEntity<Void> updateSessionMetrics(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody UpdateSessionMetricsRequest request) {
        
        log.info("Updating metrics for session {}", sessionId);
        
        UpdateSessionMetricsCommand command = new UpdateSessionMetricsCommand(
            sessionId,
            principal.getUserId(),
            request.getTotalBytesUploaded(),
            request.getAvgUploadDurationMs(),
            request.getAvgThroughputMbps(),
            request.getMinUploadDurationMs(),
            request.getMaxUploadDurationMs()
        );
        
        updateSessionMetricsHandler.handle(command);
        
        return ResponseEntity.noContent().build();
    }
}
