package com.rapidphotoupload.application.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Query to retrieve upload session status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetUploadStatusQuery {
    
    private UUID sessionId;
    private UUID userId;
}
