package com.rapidphotoupload.infrastructure.persistence.jpa;

import com.rapidphotoupload.domain.model.SessionStatus;
import com.rapidphotoupload.infrastructure.persistence.entity.UploadSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for UploadSessionEntity.
 */
@Repository
public interface JpaUploadSessionRepository extends JpaRepository<UploadSessionEntity, UUID> {
    
    Optional<UploadSessionEntity> findBySessionToken(String sessionToken);
    
    List<UploadSessionEntity> findByUserId(UUID userId);
    
    List<UploadSessionEntity> findByUserIdAndStatus(UUID userId, SessionStatus status);
}
