package com.rapidphotoupload.application.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Query to retrieve all upload sessions for a user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetUserSessionsQuery {
    
    private UUID userId;
}
