package com.rapidphotoupload.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to update performance metrics for an upload session.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSessionMetricsRequest {
    
    @NotNull
    @Positive
    private Long totalBytesUploaded;
    
    @Positive
    private Long avgUploadDurationMs;
    
    @Positive
    private Double avgThroughputMbps;
    
    @Positive
    private Long minUploadDurationMs;
    
    @Positive
    private Long maxUploadDurationMs;
}
