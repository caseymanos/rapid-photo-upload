package com.rapidphotoupload.application.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Query to retrieve photos for a user.
 * Supports filtering by upload session.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetPhotosQuery {
    
    private UUID userId;
    private UUID uploadSessionId;
}
