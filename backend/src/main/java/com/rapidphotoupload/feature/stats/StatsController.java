package com.rapidphotoupload.feature.stats;

import com.rapidphotoupload.application.dto.UploadStatsResponse;
import com.rapidphotoupload.application.handler.GetUploadStatsQueryHandler;
import com.rapidphotoupload.application.query.GetUploadStatsQuery;
import com.rapidphotoupload.infrastructure.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for upload statistics.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {
    
    private final GetUploadStatsQueryHandler getUploadStatsQueryHandler;
    
    /**
     * Get upload statistics for the authenticated user.
     * GET /api/v1/stats/uploads
     */
    @GetMapping("/uploads")
    public ResponseEntity<UploadStatsResponse> getUploadStats(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "10") int recentSessionLimit) {
        
        log.debug("Fetching upload stats for user {}", principal.getUserId());
        
        GetUploadStatsQuery query = new GetUploadStatsQuery(
            principal.getUserId(),
            recentSessionLimit
        );
        
        UploadStatsResponse response = getUploadStatsQueryHandler.handle(query);
        
        return ResponseEntity.ok(response);
    }
}
