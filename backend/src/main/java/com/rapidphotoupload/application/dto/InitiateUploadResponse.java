package com.rapidphotoupload.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response containing presigned URLs for upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateUploadResponse {
    
    private UUID photoId;
    private String s3Key;
    private String multipartUploadId;
    private List<PresignedPartUrl> presignedUrls;
    private Instant expiresAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresignedPartUrl {
        private int partNumber;
        private String url;
    }
}
