package com.rapidphotoupload.infrastructure.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("thumbnail-backfill")
@RequiredArgsConstructor
public class ThumbnailBackfillRunner implements CommandLineRunner {

    private final PhotoDerivativeBackfillService backfillService;

    @Override
    public void run(String... args) {
        log.info("Starting thumbnail backfill job");
        PhotoDerivativeBackfillService.BackfillResult result = backfillService.backfillAllMissing();
        log.info("Thumbnail backfill complete: {} photos processed, {} failures", result.processedCount(), result.failedPhotoIds().size());

        if (!result.failedPhotoIds().isEmpty()) {
            log.warn("Failed photo IDs: {}", result.failedPhotoIds());
        }
    }
}
