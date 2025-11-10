package com.rapidphotoupload.application.query;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Query to retrieve photos for a user.
 * Supports filtering by upload session.
 */
@Data
@NoArgsConstructor
public class GetPhotosQuery {
    
    private UUID userId;
    private UUID uploadSessionId;
    private boolean includeDownloadUrl;
    private int page = 0;
    private int size = 30;

    public GetPhotosQuery(UUID userId, UUID uploadSessionId) {
        this(userId, uploadSessionId, true);
    }

    public GetPhotosQuery(UUID userId, UUID uploadSessionId, boolean includeDownloadUrl) {
        this(userId, uploadSessionId, includeDownloadUrl, 0, 30);
    }

    public GetPhotosQuery(UUID userId, UUID uploadSessionId, boolean includeDownloadUrl, int page, int size) {
        this.userId = userId;
        this.uploadSessionId = uploadSessionId;
        this.includeDownloadUrl = includeDownloadUrl;
        this.page = page;
        this.size = size;
    }
}
