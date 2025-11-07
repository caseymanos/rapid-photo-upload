  package com.rapidphotoupload.infrastructure.event;

  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.messaging.simp.SimpMessagingTemplate;
  import org.springframework.stereotype.Service;

  import java.util.UUID;

  /**
   * Service for publishing real-time upload progress updates to clients via WebSocket.
   * Enables live progress tracking for the UI as required by the PRD.
   */
  @Slf4j
  @Service
  @RequiredArgsConstructor
  public class UploadProgressPublisher {

      private final SimpMessagingTemplate messagingTemplate;

      /**
       * Notify a specific user about upload progress.
       * @param userId the user ID
       * @param photoId the photo ID
       * @param progress progress percentage (0-100)
       * @param status current status (UPLOADING, COMPLETED, FAILED)
       */
      public void notifyProgress(UUID userId, UUID photoId, int progress, String status) {
          ProgressUpdate update = new ProgressUpdate(photoId, progress, status);

          messagingTemplate.convertAndSendToUser(
              userId.toString(),
              "/queue/upload-progress",
              update
          );

          log.debug("Published progress update for photo {} to user {}: {}%",
              photoId, userId, progress);
      }

      /**
       * Notify about upload session progress.
       * @param userId the user ID
       * @param sessionId the session ID
       * @param totalPhotos total photos in session
       * @param completedPhotos number of completed photos
       * @param failedPhotos number of failed photos
       */
      public void notifySessionProgress(
              UUID userId,
              UUID sessionId,
              int totalPhotos,
              int completedPhotos,
              int failedPhotos) {

          SessionProgressUpdate update = new SessionProgressUpdate(
              sessionId,
              totalPhotos,
              completedPhotos,
              failedPhotos
          );

          messagingTemplate.convertAndSendToUser(
              userId.toString(),
              "/queue/session-progress",
              update
          );

          log.debug("Published session progress for {}: {}/{} completed",
              sessionId, completedPhotos, totalPhotos);
      }

      /**
       * Progress update DTO for individual photos.
       */
      public record ProgressUpdate(
          UUID photoId,
          int progress,
          String status
      ) {}

      /**
       * Progress update DTO for upload sessions.
       */
      public record SessionProgressUpdate(
          UUID sessionId,
          int totalPhotos,
          int completedPhotos,
          int failedPhotos
      ) {}
  }