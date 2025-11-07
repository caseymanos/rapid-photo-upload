package com.rapidphotoupload.application.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Command to initiate a photo upload.
 * Represents the intent to start uploading a photo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateUploadCommand {
    
    private UUID userId;
    private String originalFilename;
    private String mimeType;
    private Long fileSizeBytes;
    private UUID uploadSessionId;
    private List<String> tags;
}
