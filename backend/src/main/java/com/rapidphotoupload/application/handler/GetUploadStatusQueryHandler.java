package com.rapidphotoupload.application.handler;

import com.rapidphotoupload.application.dto.SessionStatusResponse;
import com.rapidphotoupload.application.query.GetUploadStatusQuery;
import com.rapidphotoupload.domain.model.UploadSession;
import com.rapidphotoupload.domain.repository.UploadSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for querying upload session status.
 * Retrieves current status of an upload session.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetUploadStatusQueryHandler {
    
    private final UploadSessionRepository uploadSessionRepository;
    
    @Transactional(readOnly = true)
    public SessionStatusResponse handle(GetUploadStatusQuery query) {
        log.debug("Querying session status {}", query.getSessionId());
        
        UploadSession session = uploadSessionRepository.findById(query.getSessionId())
            .orElseThrow(() -> new IllegalArgumentException("Upload session not found"));
        
        // Verify ownership
        if (!session.getUserId().equals(query.getUserId())) {
            throw new SecurityException("User does not own this session");
        }
        
        return toResponse(session);
    }
    
    private SessionStatusResponse toResponse(UploadSession session) {
        return new SessionStatusResponse(
            session.getId(),
            session.getSessionToken(),
            session.getStatus().name(),
            session.getTotalPhotos(),
            session.getCompletedPhotos(),
            session.getFailedPhotos(),
            session.getStartedAt(),
            session.getCompletedAt()
        );
    }
}
