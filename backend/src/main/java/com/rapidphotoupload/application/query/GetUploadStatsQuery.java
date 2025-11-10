package com.rapidphotoupload.application.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Query to retrieve upload statistics for a user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetUploadStatsQuery {
    
    private UUID userId;
    private int recentSessionLimit = 10;
    
    public GetUploadStatsQuery(UUID userId) {
        this.userId = userId;
    }
}
