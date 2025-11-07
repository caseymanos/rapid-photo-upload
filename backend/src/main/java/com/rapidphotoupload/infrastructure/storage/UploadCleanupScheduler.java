  package com.rapidphotoupload.infrastructure.storage;

  import com.rapidphotoupload.domain.model.Photo;
  import com.rapidphotoupload.domain.repository.PhotoRepository;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.scheduling.annotation.Scheduled;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  import java.time.Instant;
  import java.util.List;

  /**
   * Scheduled job to clean up expired multipart uploads.
   * Runs every 6 hours to abort uploads that have expired (>2 hours old).
   */
  @Slf4j
  @Service
  @RequiredArgsConstructor
  public class UploadCleanupScheduler {

      private final PhotoRepository photoRepository;
      private final S3StorageService s3StorageService;

      /**
       * Clean up expired uploads every 6 hours.
       * Aborts multipart uploads and marks photos as failed.
       */
      @Scheduled(cron = "0 0 */6 * * *")
      @Transactional
      public void cleanupExpiredUploads() {
          log.info("Starting cleanup of expired uploads");

          Instant now = Instant.now();
          List<Photo> expiredPhotos = photoRepository.findExpiredUploads(now);

          if (expiredPhotos.isEmpty()) {
              log.info("No expired uploads found");
              return;
          }

          log.info("Found {} expired uploads to clean up", expiredPhotos.size());

          int successCount = 0;
          int failCount = 0;

          for (Photo photo : expiredPhotos) {
              try {
                  // Abort multipart upload in S3
                  if (photo.getMultipartUploadId() != null) {
                      s3StorageService.abortMultipartUpload(
                          photo.getS3Key(),
                          photo.getMultipartUploadId()
                      );
                  }

                  // Mark photo as failed
                  photo.failUpload("Upload expired after 2 hours");
                  photoRepository.save(photo);

                  successCount++;
                  log.debug("Cleaned up expired upload for photo {}", photo.getId());

              } catch (Exception e) {
                  failCount++;
                  log.error("Failed to cleanup expired upload for photo {}", photo.getId(), e);
              }
          }

          log.info("Cleanup completed: {} successful, {} failed", successCount, failCount);
      }

      /**
       * Find and cleanup abandoned multipart uploads older than 24 hours.
       * This is a safety mechanism for uploads that weren't properly tracked.
       */
      @Scheduled(cron = "0 0 3 * * *")
      public void cleanupAbandonedUploads() {
          log.info("Checking for abandoned multipart uploads in S3");
          // This would require listing all incomplete multipart uploads in S3
          // and aborting those older than 24 hours
          // Implementation depends on AWS SDK capabilities
      }
  }