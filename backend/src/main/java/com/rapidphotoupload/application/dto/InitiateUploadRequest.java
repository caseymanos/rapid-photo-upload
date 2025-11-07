package com.rapidphotoupload.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request to initiate a photo upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateUploadRequest {
    
    @NotBlank(message = "Original filename is required")
    private String originalFilename;
    
    @NotBlank(message = "MIME type is required")
    private String mimeType;
    
    @NotNull(message = "File size is required")
    @Min(value = 1, message = "File size must be greater than 0")
    private Long fileSizeBytes;
    
    private UUID uploadSessionId;
    
    private List<String> tags;
}
