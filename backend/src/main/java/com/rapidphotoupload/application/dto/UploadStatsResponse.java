package com.rapidphotoupload.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response containing aggregate upload statistics for a user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadStatsResponse {
    
    // Aggregate statistics
    private AggregateStats aggregate;
    
    // Recent upload sessions
    private List<SessionSummary> recentSessions;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregateStats {
        private long totalPhotos;
        private long totalBytesUploaded;
        private long totalSessions;
        private long completedSessions;
        private long failedSessions;
        private Double avgUploadDurationMs;
        private Double avgThroughputMbps;
        private Double successRate;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionSummary {
        private String sessionId;
        private int totalPhotos;
        private int completedPhotos;
        private int failedPhotos;
        private String status;
        private Long totalBytesUploaded;
        private Long avgUploadDurationMs;
        private Double avgThroughputMbps;
        private Instant startedAt;
        private Instant completedAt;
    }
}
