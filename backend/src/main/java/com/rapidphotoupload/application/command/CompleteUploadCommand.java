package com.rapidphotoupload.application.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Command to complete a multipart upload.
 * Represents the intent to finalize a photo upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteUploadCommand {
    
    private UUID photoId;
    private UUID userId;
    private List<PartInfo> parts;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartInfo {
        private int partNumber;
        private String etag;
        private long sizeBytes;
    }
}
