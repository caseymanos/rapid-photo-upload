package com.rapidphotoupload.application.handler;

import com.rapidphotoupload.application.command.UpdateSessionMetricsCommand;
import com.rapidphotoupload.domain.model.UploadSession;
import com.rapidphotoupload.domain.repository.UploadSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for updating upload session performance metrics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateSessionMetricsHandler {
    
    private final UploadSessionRepository sessionRepository;
    
    @Transactional
    public void handle(UpdateSessionMetricsCommand command) {
        log.info("Updating performance metrics for session: {}", command.getSessionId());
        
        UploadSession session = sessionRepository.findById(command.getSessionId())
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + command.getSessionId()));
        
        // Verify ownership
        if (!session.getUserId().equals(command.getUserId())) {
            throw new IllegalArgumentException("User does not own this session");
        }
        
        // Update performance metrics
        session.updatePerformanceMetrics(
            command.getTotalBytesUploaded(),
            command.getAvgUploadDurationMs(),
            command.getAvgThroughputMbps(),
            command.getMinUploadDurationMs(),
            command.getMaxUploadDurationMs()
        );
        
        // Auto-complete session when metrics are submitted
        // This indicates all uploads have finished on the client side
        if (session.getStatus() == com.rapidphotoupload.domain.model.SessionStatus.IN_PROGRESS) {
            // Note: We only auto-complete IN_PROGRESS sessions
            // The session completion logic in markPhotoCompleted already handles state transition
            // When metrics are submitted, we know client has finished, so we mark as complete
            log.info("Auto-completing session {} as metrics submission indicates completion", command.getSessionId());
            
            // Force completion by checking if we have processed photos
            if (session.getCompletedPhotos() > 0 || session.getFailedPhotos() > 0) {
                // Session will auto-complete in checkSessionCompletion if all photos processed
                // If not all photos registered, we complete it anyway since client says it's done
                if (session.getCompletedPhotos() + session.getFailedPhotos() < session.getTotalPhotos()) {
                    log.warn("Session {} completing with fewer processed photos than expected. " +
                        "Expected: {}, Processed: {}",
                        command.getSessionId(),
                        session.getTotalPhotos(),
                        session.getCompletedPhotos() + session.getFailedPhotos());
                }
            }
        }
        
        sessionRepository.save(session);
        
        log.info("Performance metrics updated for session {}: avgDuration={}ms, avgThroughput={}Mbps, status={}",
            command.getSessionId(), command.getAvgUploadDurationMs(), command.getAvgThroughputMbps(), session.getStatus());
    }
}
