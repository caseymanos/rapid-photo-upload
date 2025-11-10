package com.rapidphotoupload.infrastructure.persistence;

import com.rapidphotoupload.domain.model.SessionStatus;
import com.rapidphotoupload.domain.model.UploadSession;
import com.rapidphotoupload.domain.repository.UploadSessionRepository;
import com.rapidphotoupload.infrastructure.persistence.entity.UploadSessionEntity;
import com.rapidphotoupload.infrastructure.persistence.jpa.JpaUploadSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of UploadSessionRepository using JPA.
 * Maps between domain UploadSession and UploadSessionEntity.
 */
@Repository
@RequiredArgsConstructor
public class UploadSessionRepositoryImpl implements UploadSessionRepository {
    
    private final JpaUploadSessionRepository jpaRepository;
    
    @Override
    public Optional<UploadSession> findById(UUID sessionId) {
        return jpaRepository.findById(sessionId)
            .map(this::toDomain);
    }
    
    @Override
    public Optional<UploadSession> findBySessionToken(String sessionToken) {
        return jpaRepository.findBySessionToken(sessionToken)
            .map(this::toDomain);
    }
    
    @Override
    public List<UploadSession> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<UploadSession> findByUserIdAndStatus(UUID userId, SessionStatus status) {
        return jpaRepository.findByUserIdAndStatus(userId, status).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public UploadSession save(UploadSession session) {
        UploadSessionEntity entity = toEntity(session);
        UploadSessionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }
    
    @Override
    public void deleteById(UUID sessionId) {
        jpaRepository.deleteById(sessionId);
    }
    
    private UploadSession toDomain(UploadSessionEntity entity) {
        return new UploadSession(
            entity.getId(),
            entity.getUserId(),
            entity.getSessionToken(),
            entity.getTotalPhotos(),
            entity.getCompletedPhotos(),
            entity.getFailedPhotos(),
            entity.getStatus(),
            entity.getStartedAt(),
            entity.getCompletedAt(),
            entity.getVersion()
        );
    }

    private UploadSessionEntity toEntity(UploadSession session) {
        UploadSessionEntity entity = new UploadSessionEntity();
        entity.setId(session.getId());
        entity.setUserId(session.getUserId());
        entity.setSessionToken(session.getSessionToken());
        entity.setTotalPhotos(session.getTotalPhotos());
        entity.setCompletedPhotos(session.getCompletedPhotos());
        entity.setFailedPhotos(session.getFailedPhotos());
        entity.setStatus(session.getStatus());
        entity.setStartedAt(session.getStartedAt());
        entity.setCompletedAt(session.getCompletedAt());
        entity.setVersion(session.getVersion());
        return entity;
    }
}
