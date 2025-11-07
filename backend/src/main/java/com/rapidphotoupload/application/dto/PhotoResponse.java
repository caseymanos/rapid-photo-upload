package com.rapidphotoupload.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response containing photo details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoResponse {
    
    private UUID id;
    private UUID userId;
    private UUID uploadSessionId;
    private String s3Key;
    private String s3Bucket;
    private String originalFilename;
    private Long fileSizeBytes;
    private String mimeType;
    private String uploadStatus;
    private List<String> tags;
    private String thumbnailUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
