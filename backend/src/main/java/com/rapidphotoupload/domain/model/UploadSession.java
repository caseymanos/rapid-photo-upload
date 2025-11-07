package com.rapidphotoupload.domain.model;

import com.rapidphotoupload.domain.event.DomainEvent;
import com.rapidphotoupload.domain.event.UploadSessionCompleted;
import com.rapidphotoupload.domain.event.UploadSessionStarted;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class UploadSession {
    
    private UUID id;
    private UUID userId;
    private String sessionToken;
    
    private int totalPhotos;
    private int completedPhotos;
    private int failedPhotos;
    
    private SessionStatus status;
    
    private Instant startedAt;
    private Instant completedAt;
    
    private Long version;
    
    private List<DomainEvent> domainEvents = new ArrayList<>();
    
    public UploadSession(UUID id, UUID userId) {
        this.id = id;
        this.userId = userId;
        this.sessionToken = generateSessionToken();
        this.status = SessionStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
        this.totalPhotos = 0;
        this.completedPhotos = 0;
        this.failedPhotos = 0;
        this.version = 0L;

        addDomainEvent(new UploadSessionStarted(id, userId, Instant.now()));
    }

    // Public constructor for persistence reconstruction
    public UploadSession(UUID id, UUID userId, String sessionToken, int totalPhotos,
                         int completedPhotos, int failedPhotos, SessionStatus status,
                         Instant startedAt, Instant completedAt) {
        this.id = id;
        this.userId = userId;
        this.sessionToken = sessionToken;
        this.totalPhotos = totalPhotos;
        this.completedPhotos = completedPhotos;
        this.failedPhotos = failedPhotos;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.version = 0L;
    }
    
    public void registerPhoto() {
        if (this.status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot add photos to session in status: " + this.status);
        }
        this.totalPhotos++;
    }
    
    public void markPhotoCompleted() {
        this.completedPhotos++;
        checkSessionCompletion();
    }
    
    public void markPhotoFailed() {
        this.failedPhotos++;
        checkSessionCompletion();
    }
    
    private void checkSessionCompletion() {
        if (completedPhotos + failedPhotos >= totalPhotos && totalPhotos > 0) {
            completeSession();
        }
    }
    
    private void completeSession() {
        this.status = failedPhotos == totalPhotos ? SessionStatus.FAILED : SessionStatus.COMPLETED;
        this.completedAt = Instant.now();
        
        addDomainEvent(new UploadSessionCompleted(
            this.id,
            this.userId,
            this.totalPhotos,
            this.completedPhotos,
            this.failedPhotos,
            Instant.now()
        ));
    }
    
    public void cancel() {
        if (this.status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot cancel session in status: " + this.status);
        }
        this.status = SessionStatus.CANCELLED;
        this.completedAt = Instant.now();
    }
    
    private String generateSessionToken() {
        return UUID.randomUUID().toString();
    }
    
    private void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }
    
    public List<DomainEvent> getDomainEvents() {
        return List.copyOf(domainEvents);
    }
    
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}