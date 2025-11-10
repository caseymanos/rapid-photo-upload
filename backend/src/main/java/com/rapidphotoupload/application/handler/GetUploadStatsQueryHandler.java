package com.rapidphotoupload.application.handler;

import com.rapidphotoupload.application.dto.UploadStatsResponse;
import com.rapidphotoupload.application.query.GetUploadStatsQuery;
import com.rapidphotoupload.domain.model.UploadSession;
import com.rapidphotoupload.domain.repository.PhotoRepository;
import com.rapidphotoupload.domain.repository.UploadSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query handler for retrieving upload statistics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetUploadStatsQueryHandler {
    
    private final UploadSessionRepository sessionRepository;
    private final PhotoRepository photoRepository;
    
    @Transactional(readOnly = true)
    public UploadStatsResponse handle(GetUploadStatsQuery query) {
        log.debug("Fetching upload stats for user: {}", query.getUserId());
        
        // Get all sessions for the user
        List<UploadSession> allSessions = sessionRepository.findByUserId(query.getUserId());
        
        // Compute aggregate statistics
        UploadStatsResponse.AggregateStats aggregateStats = computeAggregateStats(allSessions);
        
        // Get recent sessions (limited)
        List<UploadStatsResponse.SessionSummary> recentSessions = allSessions.stream()
            .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
            .limit(query.getRecentSessionLimit())
            .map(this::toSessionSummary)
            .collect(Collectors.toList());
        
        return new UploadStatsResponse(aggregateStats, recentSessions);
    }
    
    private UploadStatsResponse.AggregateStats computeAggregateStats(List<UploadSession> sessions) {
        if (sessions.isEmpty()) {
            return new UploadStatsResponse.AggregateStats(0L, 0L, 0L, 0L, 0L, null, null, 0.0);
        }
        
        long totalPhotos = sessions.stream().mapToLong(UploadSession::getTotalPhotos).sum();
        long totalBytes = sessions.stream()
            .filter(s -> s.getTotalBytesUploaded() != null)
            .mapToLong(UploadSession::getTotalBytesUploaded)
            .sum();
        
        long totalSessions = sessions.size();
        long completedSessions = sessions.stream()
            .filter(s -> "COMPLETED".equals(s.getStatus().name()))
            .count();
        long failedSessions = sessions.stream()
            .filter(s -> "FAILED".equals(s.getStatus().name()))
            .count();
        
        // Average metrics from sessions with data
        List<UploadSession> sessionsWithMetrics = sessions.stream()
            .filter(s -> s.getAvgUploadDurationMs() != null && s.getAvgThroughputMbps() != null)
            .collect(Collectors.toList());
        
        Double avgDuration = null;
        Double avgThroughput = null;
        
        if (!sessionsWithMetrics.isEmpty()) {
            avgDuration = sessionsWithMetrics.stream()
                .mapToLong(UploadSession::getAvgUploadDurationMs)
                .average()
                .orElse(0.0);
            
            avgThroughput = sessionsWithMetrics.stream()
                .mapToDouble(UploadSession::getAvgThroughputMbps)
                .average()
                .orElse(0.0);
        }
        
        // Success rate
        long completedPhotos = sessions.stream().mapToLong(UploadSession::getCompletedPhotos).sum();
        double successRate = totalPhotos > 0 ? (completedPhotos * 100.0 / totalPhotos) : 0.0;
        
        return new UploadStatsResponse.AggregateStats(
            totalPhotos,
            totalBytes,
            totalSessions,
            completedSessions,
            failedSessions,
            avgDuration,
            avgThroughput,
            Math.round(successRate * 100.0) / 100.0
        );
    }
    
    private UploadStatsResponse.SessionSummary toSessionSummary(UploadSession session) {
        return new UploadStatsResponse.SessionSummary(
            session.getId().toString(),
            session.getTotalPhotos(),
            session.getCompletedPhotos(),
            session.getFailedPhotos(),
            session.getStatus().name(),
            session.getTotalBytesUploaded(),
            session.getAvgUploadDurationMs(),
            session.getAvgThroughputMbps(),
            session.getStartedAt(),
            session.getCompletedAt()
        );
    }
}
