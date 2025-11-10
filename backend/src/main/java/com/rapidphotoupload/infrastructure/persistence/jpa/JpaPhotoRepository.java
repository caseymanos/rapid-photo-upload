package com.rapidphotoupload.infrastructure.persistence.jpa;

import com.rapidphotoupload.domain.model.UploadStatus;
import com.rapidphotoupload.infrastructure.persistence.entity.PhotoEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for PhotoEntity.
 */
@Repository
public interface JpaPhotoRepository extends JpaRepository<PhotoEntity, UUID> {
    
    List<PhotoEntity> findByUserId(UUID userId);
    
    List<PhotoEntity> findByUploadSessionId(UUID sessionId);
    
    List<PhotoEntity> findByUserIdAndUploadStatus(UUID userId, UploadStatus status);
    
    @Query("SELECT p FROM PhotoEntity p WHERE p.uploadStatus IN ('INITIATED', 'UPLOADING') " +
           "AND p.uploadExpiresAt < :expirationTime")
    List<PhotoEntity> findExpiredUploads(@Param("expirationTime") Instant expirationTime);

    @Query("""
        SELECT p FROM PhotoEntity p
        WHERE p.uploadStatus = :status
          AND (
                p.thumbnailUrl IS NULL
             OR p.thumbnailFallbackUrl IS NULL
             OR p.placeholderUrl IS NULL
             OR p.placeholderFallbackUrl IS NULL
             OR p.placeholderBase64 IS NULL
          )
        ORDER BY p.createdAt ASC
        """)
    List<PhotoEntity> findCompletedWithoutDerivatives(@Param("status") UploadStatus status, Pageable pageable);
}
