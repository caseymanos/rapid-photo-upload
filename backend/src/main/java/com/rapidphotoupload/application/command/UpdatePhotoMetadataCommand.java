package com.rapidphotoupload.application.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Command to update photo metadata.
 * Represents the intent to modify photo tags or metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePhotoMetadataCommand {
    
    private UUID photoId;
    private UUID userId;
    private List<String> tags;
}
