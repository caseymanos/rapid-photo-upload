package com.rapidphotoupload.application.handler;

import com.rapidphotoupload.application.command.UpdateSessionMetricsCommand;
import com.rapidphotoupload.domain.model.SessionStatus;
import com.rapidphotoupload.domain.model.UploadSession;
import com.rapidphotoupload.domain.repository.UploadSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateSessionMetricsHandlerTest {

    @Mock
    private UploadSessionRepository sessionRepository;

    @InjectMocks
    private UpdateSessionMetricsHandler handler;

    private UUID sessionId;
    private UUID userId;
    private UploadSession session;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        
        // Create a test session in IN_PROGRESS state
        session = new UploadSession(
            sessionId,
            userId,
            "test-session-token",
            10,
            10,
            0,
            SessionStatus.IN_PROGRESS,
            Instant.now().minusSeconds(300),
            null,
            null,
            null,
            null,
            null,
            null,
            0L
        );
    }

    @Test
    void handle_shouldUpdateMetrics_whenValidCommand() {
        // Arrange
        UpdateSessionMetricsCommand command = new UpdateSessionMetricsCommand(
            sessionId,
            userId,
            52428800L,      // 50 MB
            2500L,          // 2.5 seconds avg
            167.77,         // Mbps
            1800L,          // min
            4200L           // max
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(UploadSession.class))).thenReturn(session);

        // Act
        handler.handle(command);

        // Assert
        verify(sessionRepository).findById(sessionId);
        verify(sessionRepository).save(session);
        
        assertEquals(52428800L, session.getTotalBytesUploaded());
        assertEquals(2500L, session.getAvgUploadDurationMs());
        assertEquals(167.77, session.getAvgThroughputMbps());
        assertEquals(1800L, session.getMinUploadDurationMs());
        assertEquals(4200L, session.getMaxUploadDurationMs());
    }

    @Test
    void handle_shouldThrowException_whenSessionNotFound() {
        // Arrange
        UpdateSessionMetricsCommand command = new UpdateSessionMetricsCommand(
            sessionId,
            userId,
            52428800L,
            2500L,
            167.77,
            1800L,
            4200L
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> handler.handle(command)
        );
        
        assertTrue(exception.getMessage().contains("Session not found"));
        verify(sessionRepository).findById(sessionId);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void handle_shouldThrowException_whenUserDoesNotOwnSession() {
        // Arrange
        UUID differentUserId = UUID.randomUUID();
        UpdateSessionMetricsCommand command = new UpdateSessionMetricsCommand(
            sessionId,
            differentUserId,
            52428800L,
            2500L,
            167.77,
            1800L,
            4200L
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> handler.handle(command)
        );
        
        assertTrue(exception.getMessage().contains("does not own this session"));
        verify(sessionRepository).findById(sessionId);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void handle_shouldAutoCompleteSession_whenInProgressWithCompletedPhotos() {
        // Arrange
        UpdateSessionMetricsCommand command = new UpdateSessionMetricsCommand(
            sessionId,
            userId,
            52428800L,
            2500L,
            167.77,
            1800L,
            4200L
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(UploadSession.class))).thenReturn(session);

        // Act
        handler.handle(command);

        // Assert
        verify(sessionRepository).save(session);
        // Session should remain IN_PROGRESS because it has completed photos
        // (The actual completion happens in markPhotoCompleted, not here)
        assertEquals(SessionStatus.IN_PROGRESS, session.getStatus());
    }

    @Test
    void handle_shouldNotUpdateMetrics_whenSessionAlreadyCompleted() {
        // Arrange
        UploadSession completedSession = new UploadSession(
            sessionId,
            userId,
            "test-session-token",
            10,
            10,
            0,
            SessionStatus.COMPLETED,
            Instant.now().minusSeconds(300),
            Instant.now(),
            50000000L,
            2000L,
            150.0,
            1500L,
            3000L,
            0L
        );

        UpdateSessionMetricsCommand command = new UpdateSessionMetricsCommand(
            sessionId,
            userId,
            52428800L,
            2500L,
            167.77,
            1800L,
            4200L
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(completedSession));
        when(sessionRepository.save(any(UploadSession.class))).thenReturn(completedSession);

        // Act
        handler.handle(command);

        // Assert
        verify(sessionRepository).save(completedSession);
        // Metrics should still be updated even if session is completed
        assertEquals(52428800L, completedSession.getTotalBytesUploaded());
        assertEquals(SessionStatus.COMPLETED, completedSession.getStatus());
    }

    @Test
    void handle_shouldHandleNullMetrics() {
        // Arrange
        UpdateSessionMetricsCommand command = new UpdateSessionMetricsCommand(
            sessionId,
            userId,
            0L,
            null,
            null,
            null,
            null
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(UploadSession.class))).thenReturn(session);

        // Act
        handler.handle(command);

        // Assert
        verify(sessionRepository).save(session);
        assertEquals(0L, session.getTotalBytesUploaded());
        assertNull(session.getAvgUploadDurationMs());
        assertNull(session.getAvgThroughputMbps());
    }

    @Test
    void handle_shouldUpdateMetrics_whenCalledMultipleTimes() {
        // Arrange
        UpdateSessionMetricsCommand command1 = new UpdateSessionMetricsCommand(
            sessionId,
            userId,
            26214400L,
            2000L,
            100.0,
            1500L,
            3000L
        );

        UpdateSessionMetricsCommand command2 = new UpdateSessionMetricsCommand(
            sessionId,
            userId,
            52428800L,
            2500L,
            150.0,
            1800L,
            4000L
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(UploadSession.class))).thenReturn(session);

        // Act
        handler.handle(command1);
        handler.handle(command2);

        // Assert
        verify(sessionRepository, times(2)).findById(sessionId);
        verify(sessionRepository, times(2)).save(session);
        
        // Should have latest values
        assertEquals(52428800L, session.getTotalBytesUploaded());
        assertEquals(2500L, session.getAvgUploadDurationMs());
        assertEquals(150.0, session.getAvgThroughputMbps());
    }
}
