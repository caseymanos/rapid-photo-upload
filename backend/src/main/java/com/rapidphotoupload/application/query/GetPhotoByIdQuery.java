package com.rapidphotoupload.application.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Query to retrieve a single photo by ID.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetPhotoByIdQuery {
    
    private UUID photoId;
    private UUID userId;
}
