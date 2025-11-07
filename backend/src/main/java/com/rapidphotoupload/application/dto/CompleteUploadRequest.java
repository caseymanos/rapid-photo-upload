package com.rapidphotoupload.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to complete a multipart upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteUploadRequest {
    
    @NotEmpty(message = "Upload parts are required")
    private List<UploadPartInfo> parts;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadPartInfo {
        private int partNumber;
        
        @NotBlank(message = "ETag is required")
        private String etag;
        
        private long sizeBytes;
    }
}
