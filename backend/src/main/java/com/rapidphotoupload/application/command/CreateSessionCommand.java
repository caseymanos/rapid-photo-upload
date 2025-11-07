package com.rapidphotoupload.application.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Command to create a new upload session.
 * Represents the intent to start a batch upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionCommand {
    
    private UUID userId;
    private int expectedPhotoCount;
}
