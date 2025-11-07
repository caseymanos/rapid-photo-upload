package com.rapidphotoupload.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response containing upload session status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatusResponse {
    
    private UUID sessionId;
    private String sessionToken;
    private String status;
    private int totalPhotos;
    private int completedPhotos;
    private int failedPhotos;
    private Instant startedAt;
    private Instant completedAt;
}
