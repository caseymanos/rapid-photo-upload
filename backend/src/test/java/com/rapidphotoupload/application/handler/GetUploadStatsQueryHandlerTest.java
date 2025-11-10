package com.rapidphotoupload.application.handler;

import com.rapidphotoupload.application.dto.UploadStatsResponse;
import com.rapidphotoupload.application.query.GetUploadStatsQuery;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetUploadStatsQueryHandlerTest {

    @Mock
    private UploadSessionRepository sessionRepository;

    @InjectMocks
    private GetUploadStatsQueryHandler handler;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void handle_shouldReturnEmptyStats_whenNoSessions() {
        // Arrange
        GetUploadStatsQuery query = new GetUploadStatsQuery(userId, 10);
        when(sessionRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // Act
        UploadStatsResponse response = handler.handle(query);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getAggregate());
        assertEquals(0, response.getAggregate().getTotalPhotos());
        assertEquals(0, response.getAggregate().getTotalBytesUploaded());
        assertEquals(0, response.getAggregate().getTotalSessions());
        assertEquals(0, response.getAggregate().getCompletedSessions());
        assertNull(response.getAggregate().getAvgUploadDurationMs());
        assertNull(response.getAggregate().getAvgThroughputMbps());
        assertEquals(0, response.getRecentSessions().size());
        
        verify(sessionRepository).findByUserId(userId);
    }

    @Test
    void handle_shouldComputeAggregateStats_withSingleSession() {
        // Arrange
        GetUploadStatsQuery query = new GetUploadStatsQuery(userId, 10);
        
        UploadSession session = createSession(
            10, 10, 0, SessionStatus.COMPLETED,
            52428800L, 2500L, 167.77, 1800L, 4200L
        );
        
        when(sessionRepository.findByUserId(userId)).thenReturn(Collections.singletonList(session));

        // Act
        UploadStatsResponse response = handler.handle(query);

        // Assert
        assertNotNull(response);
        UploadStatsResponse.AggregateStats aggregate = response.getAggregate();
        
        assertEquals(10, aggregate.getTotalPhotos());
        assertEquals(52428800L, aggregate.getTotalBytesUploaded());
        assertEquals(1, aggregate.getTotalSessions());
        assertEquals(1, aggregate.getCompletedSessions());
        assertEquals(2500.0, aggregate.getAvgUploadDurationMs());
        assertEquals(167.77, aggregate.getAvgThroughputMbps());
        assertEquals(100.0, aggregate.getSuccessRate());
        
        assertEquals(1, response.getRecentSessions().size());
    }

    @Test
    void handle_shouldComputeAggregateStats_withMultipleSessions() {
        // Arrange
        GetUploadStatsQuery query = new GetUploadStatsQuery(userId, 10);
        
        List<UploadSession> sessions = Arrays.asList(
            createSession(10, 10, 0, SessionStatus.COMPLETED, 52428800L, 2500L, 167.77, 1800L, 4200L),
            createSession(5, 4, 1, SessionStatus.COMPLETED, 26214400L, 3000L, 69.91, 2100L, 4800L),
            createSession(3, 3, 0, SessionStatus.COMPLETED, 15728640L, 1800L, 69.91, 1500L, 2200L)
        );
        
        when(sessionRepository.findByUserId(userId)).thenReturn(sessions);

        // Act
        UploadStatsResponse response = handler.handle(query);

        // Assert
        assertNotNull(response);
        UploadStatsResponse.AggregateStats aggregate = response.getAggregate();
        
        assertEquals(17, aggregate.getTotalPhotos()); // 10 + 4 + 3 (only completed)
        assertEquals(94371840L, aggregate.getTotalBytesUploaded()); // Sum of all
        assertEquals(3, aggregate.getTotalSessions());
        assertEquals(3, aggregate.getCompletedSessions());
        
        // Average duration: (2500 + 3000 + 1800) / 3 = 2433.33
        assertEquals(2433.33, aggregate.getAvgUploadDurationMs(), 0.01);
        
        // Average throughput: (167.77 + 69.91 + 69.91) / 3 = 102.53
        assertEquals(102.53, aggregate.getAvgThroughputMbps(), 0.01);
        
        // Success rate: 17 / 18 * 100 = 94.44%
        assertEquals(94.44, aggregate.getSuccessRate(), 0.01);
        
        assertEquals(3, response.getRecentSessions().size());
    }

    @Test
    void handle_shouldHandleFailedSessions() {
        // Arrange
        GetUploadStatsQuery query = new GetUploadStatsQuery(userId, 10);
        
        List<UploadSession> sessions = Arrays.asList(
            createSession(10, 10, 0, SessionStatus.COMPLETED, 52428800L, 2500L, 167.77, 1800L, 4200L),
            createSession(8, 3, 5, SessionStatus.FAILED, 15728640L, 2800L, 44.98, 2500L, 3100L)
        );
        
        when(sessionRepository.findByUserId(userId)).thenReturn(sessions);

        // Act
        UploadStatsResponse response = handler.handle(query);

        // Assert
        UploadStatsResponse.AggregateStats aggregate = response.getAggregate();
        
        assertEquals(13, aggregate.getTotalPhotos()); // 10 + 3 (only completed)
        assertEquals(2, aggregate.getTotalSessions());
        assertEquals(1, aggregate.getCompletedSessions());
        assertEquals(1, aggregate.getFailedSessions());
        
        // Success rate: 13 / 18 * 100 = 72.22%
        assertEquals(72.22, aggregate.getSuccessRate(), 0.01);
    }

    @Test
    void handle_shouldExcludeInProgressSessions_fromCompletedStats() {
        // Arrange
        GetUploadStatsQuery query = new GetUploadStatsQuery(userId, 10);
        
        List<UploadSession> sessions = Arrays.asList(
            createSession(10, 10, 0, SessionStatus.COMPLETED, 52428800L, 2500L, 167.77, 1800L, 4200L),
            createSession(5, 2, 0, SessionStatus.IN_PROGRESS, 10485760L, 2200L, 38.15, 2000L, 2400L)
        );
        
        when(sessionRepository.findByUserId(userId)).thenReturn(sessions);

        // Act
        UploadStatsResponse response = handler.handle(query);

        // Assert
        UploadStatsResponse.AggregateStats aggregate = response.getAggregate();
        
        assertEquals(10, aggregate.getTotalPhotos()); // Only from completed sessions
        assertEquals(2, aggregate.getTotalSessions());
        assertEquals(1, aggregate.getCompletedSessions());
        assertEquals(0, aggregate.getFailedSessions());
    }

    @Test
    void handle_shouldLimitRecentSessions_toSpecifiedCount() {
        // Arrange
        GetUploadStatsQuery query = new GetUploadStatsQuery(userId, 2);
        
        List<UploadSession> sessions = Arrays.asList(
            createSession(10, 10, 0, SessionStatus.COMPLETED, 52428800L, 2500L, 167.77, 1800L, 4200L),
            createSession(5, 4, 1, SessionStatus.COMPLETED, 26214400L, 3000L, 69.91, 2100L, 4800L),
            createSession(3, 3, 0, SessionStatus.COMPLETED, 15728640L, 1800L, 69.91, 1500L, 2200L)
        );
        
        when(sessionRepository.findByUserId(userId)).thenReturn(sessions);

        // Act
        UploadStatsResponse response = handler.handle(query);

        // Assert
        assertEquals(3, response.getAggregate().getTotalSessions());
        assertEquals(2, response.getRecentSessions().size()); // Limited to 2
    }

    @Test
    void handle_shouldHandleSessionsWithNullMetrics() {
        // Arrange
        GetUploadStatsQuery query = new GetUploadStatsQuery(userId, 10);
        
        List<UploadSession> sessions = Arrays.asList(
            createSession(10, 10, 0, SessionStatus.COMPLETED, null, null, null, null, null),
            createSession(5, 5, 0, SessionStatus.COMPLETED, 26214400L, 3000L, 69.91, 2100L, 4800L)
        );
        
        when(sessionRepository.findByUserId(userId)).thenReturn(sessions);

        // Act
        UploadStatsResponse response = handler.handle(query);

        // Assert
        UploadStatsResponse.AggregateStats aggregate = response.getAggregate();
        
        assertEquals(15, aggregate.getTotalPhotos());
        assertEquals(26214400L, aggregate.getTotalBytesUploaded()); // Only from session with metrics
        assertEquals(2, aggregate.getTotalSessions());
        
        // Average should only include session with metrics
        assertEquals(3000.0, aggregate.getAvgUploadDurationMs());
        assertEquals(69.91, aggregate.getAvgThroughputMbps());
    }

    @Test
    void handle_shouldMapSessionDetailsCorrectly() {
        // Arrange
        GetUploadStatsQuery query = new GetUploadStatsQuery(userId, 10);
        UUID sessionId = UUID.randomUUID();
        
        UploadSession session = new UploadSession(
            sessionId,
            userId,
            "test-token-123",
            10,
            8,
            2,
            SessionStatus.COMPLETED,
            Instant.parse("2025-11-10T10:00:00Z"),
            Instant.parse("2025-11-10T10:05:00Z"),
            52428800L,
            2500L,
            167.77,
            1800L,
            4200L,
            0L
        );
        
        when(sessionRepository.findByUserId(userId)).thenReturn(Collections.singletonList(session));

        // Act
        UploadStatsResponse response = handler.handle(query);

        // Assert
        assertEquals(1, response.getRecentSessions().size());
        
        UploadStatsResponse.SessionSummary summary = response.getRecentSessions().get(0);
        assertEquals(sessionId.toString(), summary.getSessionId());
        assertEquals(10, summary.getTotalPhotos());
        assertEquals(8, summary.getCompletedPhotos());
        assertEquals(2, summary.getFailedPhotos());
        assertEquals("COMPLETED", summary.getStatus());
        assertEquals(52428800L, summary.getTotalBytesUploaded());
        assertEquals(2500L, summary.getAvgUploadDurationMs());
        assertEquals(167.77, summary.getAvgThroughputMbps());
        assertNotNull(summary.getStartedAt());
        assertNotNull(summary.getCompletedAt());
    }

    @Test
    void handle_shouldCalculateSuccessRate_asZero_whenNoPhotos() {
        // Arrange
        GetUploadStatsQuery query = new GetUploadStatsQuery(userId, 10);
        
        UploadSession session = createSession(
            0, 0, 0, SessionStatus.COMPLETED,
            0L, null, null, null, null
        );
        
        when(sessionRepository.findByUserId(userId)).thenReturn(Collections.singletonList(session));

        // Act
        UploadStatsResponse response = handler.handle(query);

        // Assert
        assertEquals(0.0, response.getAggregate().getSuccessRate());
    }

    private UploadSession createSession(
        int totalPhotos, int completedPhotos, int failedPhotos, 
        SessionStatus status, Long totalBytes, Long avgDuration, 
        Double avgThroughput, Long minDuration, Long maxDuration
    ) {
        return new UploadSession(
            UUID.randomUUID(),
            userId,
            "session-token-" + UUID.randomUUID().toString().substring(0, 8),
            totalPhotos,
            completedPhotos,
            failedPhotos,
            status,
            Instant.now().minusSeconds(300),
            status == SessionStatus.COMPLETED || status == SessionStatus.FAILED 
                ? Instant.now() : null,
            totalBytes,
            avgDuration,
            avgThroughput,
            minDuration,
            maxDuration,
            0L
        );
    }
}
