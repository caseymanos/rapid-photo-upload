package com.rapidphotoupload.application.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Command to update performance metrics for an upload session.
 * Captures aggregate statistics from client-side upload tracking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSessionMetricsCommand {
    
    private UUID sessionId;
    private UUID userId;
    private Long totalBytesUploaded;
    private Long avgUploadDurationMs;
    private Double avgThroughputMbps;
    private Long minUploadDurationMs;
    private Long maxUploadDurationMs;
}
