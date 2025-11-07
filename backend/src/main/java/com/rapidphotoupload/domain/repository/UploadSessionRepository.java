package com.rapidphotoupload.domain.repository;

import com.rapidphotoupload.domain.model.SessionStatus;
import com.rapidphotoupload.domain.model.UploadSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UploadSession aggregate.
 * Defines the contract for UploadSession persistence operations.
 */
public interface UploadSessionRepository {
    
    /**
     * Find an upload session by its unique identifier.
     * @param sessionId the session ID
     * @return Optional containing the session if found
     */
    Optional<UploadSession> findById(UUID sessionId);
    
    /**
     * Find an upload session by its token.
     * @param sessionToken the session token
     * @return Optional containing the session if found
     */
    Optional<UploadSession> findBySessionToken(String sessionToken);
    
    /**
     * Find all upload sessions for a user.
     * @param userId the user ID
     * @return list of upload sessions
     */
    List<UploadSession> findByUserId(UUID userId);
    
    /**
     * Find upload sessions by status.
     * @param userId the user ID
     * @param status the session status
     * @return list of sessions matching the status
     */
    List<UploadSession> findByUserIdAndStatus(UUID userId, SessionStatus status);
    
    /**
     * Save or update an upload session.
     * @param session the session to save
     * @return the saved session
     */
    UploadSession save(UploadSession session);
    
    /**
     * Delete an upload session.
     * @param sessionId the session ID
     */
    void deleteById(UUID sessionId);
}
