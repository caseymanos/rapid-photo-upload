package com.rapidphotoupload.infrastructure.persistence;

import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.domain.model.PhotoMetadata;
import com.rapidphotoupload.domain.model.UploadStatus;
import com.rapidphotoupload.domain.repository.PhotoRepository;
import com.rapidphotoupload.infrastructure.persistence.entity.PhotoEntity;
import com.rapidphotoupload.infrastructure.persistence.jpa.JpaPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of PhotoRepository using JPA.
 * Maps between domain Photo and PhotoEntity.
 */
@Repository
@RequiredArgsConstructor
public class PhotoRepositoryImpl implements PhotoRepository {
    
    private final JpaPhotoRepository jpaRepository;
    
    @Override
    public Optional<Photo> findById(UUID photoId) {
        return jpaRepository.findById(photoId)
            .map(this::toDomain);
    }
    
    @Override
    public List<Photo> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Photo> findByUploadSessionId(UUID sessionId) {
        return jpaRepository.findByUploadSessionId(sessionId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Photo> findByUserIdAndStatus(UUID userId, UploadStatus status) {
        return jpaRepository.findByUserIdAndUploadStatus(userId, status).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Photo> findExpiredUploads(Instant expirationTime) {
        return jpaRepository.findExpiredUploads(expirationTime).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public Photo save(Photo photo) {
        PhotoEntity entity = toEntity(photo);
        PhotoEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }
    
    @Override
    public List<Photo> saveAll(Iterable<Photo> photos) {
        List<PhotoEntity> entities = ((List<Photo>) photos).stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
        
        return jpaRepository.saveAll(entities).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(UUID photoId) {
        jpaRepository.deleteById(photoId);
    }
    
    private Photo toDomain(PhotoEntity entity) {
        PhotoMetadata metadata = new PhotoMetadata(entity.getTags());
        
        Photo photo = new Photo(
            entity.getId(),
            entity.getUserId(),
            entity.getUploadSessionId(),
            entity.getS3Key(),
            entity.getS3Bucket(),
            entity.getS3Etag(),
            entity.getOriginalFilename(),
            entity.getFileSizeBytes(),
            entity.getMimeType(),
            entity.getUploadStatus(),
            entity.getMultipartUploadId(),
            entity.getUploadExpiresAt(),
            metadata,
            entity.getThumbnailUrl(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
        
        return photo;
    }
    
    private PhotoEntity toEntity(Photo photo) {
        PhotoEntity entity = new PhotoEntity();
        entity.setId(photo.getId());
        entity.setUserId(photo.getUserId());
        entity.setUploadSessionId(photo.getUploadSessionId());
        entity.setS3Key(photo.getS3Key());
        entity.setS3Bucket(photo.getS3Bucket());
        entity.setS3Etag(photo.getS3Etag());
        entity.setOriginalFilename(photo.getOriginalFilename());
        entity.setFileSizeBytes(photo.getFileSizeBytes());
        entity.setMimeType(photo.getMimeType());
        entity.setUploadStatus(photo.getUploadStatus());
        entity.setMultipartUploadId(photo.getMultipartUploadId());
        entity.setUploadExpiresAt(photo.getUploadExpiresAt());
        entity.setTags(photo.getMetadata().getTags());
        entity.setThumbnailUrl(photo.getThumbnailUrl());
        entity.setCreatedAt(photo.getCreatedAt());
        entity.setUpdatedAt(photo.getUpdatedAt());
        return entity;
    }
}
