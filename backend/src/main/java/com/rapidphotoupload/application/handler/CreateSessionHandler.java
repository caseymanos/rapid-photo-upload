package com.rapidphotoupload.application.handler;

import com.rapidphotoupload.application.command.CreateSessionCommand;
import com.rapidphotoupload.domain.model.UploadSession;
import com.rapidphotoupload.domain.repository.UploadSessionRepository;
import com.rapidphotoupload.infrastructure.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handler for creating upload sessions.
 * Initializes a new batch upload session.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateSessionHandler {
    
    private final UploadSessionRepository uploadSessionRepository;
    private final DomainEventPublisher eventPublisher;
    
    @Transactional
    public UploadSession handle(CreateSessionCommand command) {
        log.info("Creating upload session for user {}", command.getUserId());
        
        // Create new session
        UploadSession session = new UploadSession(UUID.randomUUID(), command.getUserId());
        
        // Save session
        UploadSession savedSession = uploadSessionRepository.save(session);
        
        // Publish domain events
        eventPublisher.publishAll(savedSession.getDomainEvents());
        session.clearDomainEvents();
        
        log.info("Upload session {} created for user {}", savedSession.getId(), command.getUserId());
        
        return savedSession;
    }
}
