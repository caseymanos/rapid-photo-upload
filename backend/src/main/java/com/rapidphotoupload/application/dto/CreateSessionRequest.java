package com.rapidphotoupload.application.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to create a new upload session.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {
    
    @Min(value = 1, message = "Total photos must be at least 1")
    private int expectedPhotoCount;
}
