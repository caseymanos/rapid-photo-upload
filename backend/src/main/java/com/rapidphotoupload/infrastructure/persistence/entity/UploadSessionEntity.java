package com.rapidphotoupload.infrastructure.persistence.entity;

import com.rapidphotoupload.domain.model.SessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for UploadSession.
 */
@Entity
@Table(name = "upload_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UploadSessionEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "session_token", nullable = false, unique = true)
    private String sessionToken;
    
    @Column(name = "total_photos", nullable = false)
    private Integer totalPhotos;
    
    @Column(name = "completed_photos", nullable = false)
    private Integer completedPhotos;
    
    @Column(name = "failed_photos", nullable = false)
    private Integer failedPhotos;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;
    
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Version
    private Long version;
}
