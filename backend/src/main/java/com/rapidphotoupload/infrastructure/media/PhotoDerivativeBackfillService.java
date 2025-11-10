package com.rapidphotoupload.infrastructure.media;

import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.domain.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoDerivativeBackfillService {

    private final PhotoRepository photoRepository;
    private final PhotoDerivativeService photoDerivativeService;

    @Value("${thumbnails.backfill.batch-size:50}")
    private int defaultBatchSize;

    /**
     * Iteratively processes completed photos missing derivative assets until none remain.
     * @return summary of the backfill attempt
     */
    public BackfillResult backfillAllMissing() {
        int processedCount = 0;
        Set<UUID> failed = new HashSet<>();
        int batchSize = Math.max(1, defaultBatchSize);

        while (true) {
            List<Photo> batch = photoRepository.findCompletedWithoutDerivatives(batchSize).stream()
                .filter(photo -> !failed.contains(photo.getId()))
                .toList();

            if (batch.isEmpty()) {
                break;
            }

            boolean progress = false;

            for (Photo photo : batch) {
                try {
                    photoDerivativeService.generateDerivatives(photo.getId());
                    processedCount++;
                    progress = true;
                } catch (Exception e) {
                    log.error("Failed to backfill derivatives for photo {}", photo.getId(), e);
                    failed.add(photo.getId());
                }
            }

            if (!progress) {
                log.warn("Backfill stopped because no progress was made; {} photos remain unresolved", failed.size());
                break;
            }
        }

        return new BackfillResult(processedCount, new ArrayList<>(failed));
    }

    public record BackfillResult(int processedCount, List<UUID> failedPhotoIds) {}
}
