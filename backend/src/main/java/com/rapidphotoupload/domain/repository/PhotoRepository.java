package com.rapidphotoupload.domain.repository;

import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.domain.model.UploadStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Photo aggregate.
 * Defines the contract for Photo persistence operations.
 */
public interface PhotoRepository {
    
    /**
     * Find a photo by its unique identifier.
     * @param photoId the photo ID
     * @return Optional containing the photo if found
     */
    Optional<Photo> findById(UUID photoId);
    
    /**
     * Find all photos for a user.
     * @param userId the user ID
     * @return list of photos
     */
    List<Photo> findByUserId(UUID userId);
    
    /**
     * Find all photos in a specific upload session.
     * @param sessionId the session ID
     * @return list of photos
     */
    List<Photo> findByUploadSessionId(UUID sessionId);
    
    /**
     * Find photos by status.
     * @param userId the user ID
     * @param status the upload status
     * @return list of photos matching the status
     */
    List<Photo> findByUserIdAndStatus(UUID userId, UploadStatus status);
    
    /**
     * Find expired uploads that need cleanup.
     * @param expirationTime the cutoff time
     * @return list of expired photos
     */
    List<Photo> findExpiredUploads(Instant expirationTime);
    
    /**
     * Save or update a photo.
     * @param photo the photo to save
     * @return the saved photo
     */
    Photo save(Photo photo);
    
    /**
     * Save multiple photos in a batch.
     * @param photos the photos to save
     * @return the saved photos
     */
    List<Photo> saveAll(Iterable<Photo> photos);
    
    /**
     * Delete a photo.
     * @param photoId the photo ID
     */
    void deleteById(UUID photoId);

    /**
     * Find completed uploads that do not yet have generated derivatives.
     * @param batchSize maximum number of records to return
     * @return list of photos missing derivative assets
     */
    List<Photo> findCompletedWithoutDerivatives(int batchSize);
}
