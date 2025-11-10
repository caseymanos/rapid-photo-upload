  package com.rapidphotoupload.integration;

  import com.rapidphotoupload.application.command.CompleteUploadCommand;
  import com.rapidphotoupload.application.command.CreateSessionCommand;
  import com.rapidphotoupload.application.command.InitiateUploadCommand;
  import com.rapidphotoupload.application.dto.InitiateUploadResponse;
  import com.rapidphotoupload.application.handler.CompleteUploadHandler;
  import com.rapidphotoupload.application.handler.CreateSessionHandler;
  import com.rapidphotoupload.application.handler.InitiateUploadHandler;
  import com.rapidphotoupload.domain.model.Photo;
  import com.rapidphotoupload.domain.model.UploadSession;
  import com.rapidphotoupload.domain.model.UploadStatus;
  import com.rapidphotoupload.domain.repository.PhotoRepository;
  import com.rapidphotoupload.domain.repository.UploadSessionRepository;
  import com.rapidphotoupload.domain.repository.UserRepository;
  import com.rapidphotoupload.domain.model.User;
  import com.rapidphotoupload.infrastructure.storage.S3StorageService;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.context.SpringBootTest;
  import org.springframework.boot.test.mock.mockito.MockBean;
  import org.springframework.test.context.DynamicPropertyRegistry;
  import org.springframework.test.context.DynamicPropertySource;
  import org.testcontainers.containers.PostgreSQLContainer;
  import org.testcontainers.junit.jupiter.Container;
  import org.testcontainers.junit.jupiter.Testcontainers;

  import java.util.List;
  import java.util.UUID;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.mockito.ArgumentMatchers.*;
  import static org.mockito.Mockito.when;

  /**
   * Integration test for the complete upload flow.
   * Tests from initiation through completion, including database persistence.
   * This is a MANDATORY requirement from the PRD.
   */
  @SpringBootTest
  @Testcontainers
  class UploadFlowIntegrationTest {

      @Container
      static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
          .withDatabaseName("photoupload_test")
          .withUsername("test")
          .withPassword("test");

      @DynamicPropertySource
      static void configureProperties(DynamicPropertyRegistry registry) {
          registry.add("spring.datasource.url", postgres::getJdbcUrl);
          registry.add("spring.datasource.username", postgres::getUsername);
          registry.add("spring.datasource.password", postgres::getPassword);
          registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
      }

      @Autowired
      private InitiateUploadHandler initiateUploadHandler;

      @Autowired
      private CompleteUploadHandler completeUploadHandler;

      @Autowired
      private CreateSessionHandler createSessionHandler;

      @Autowired
      private PhotoRepository photoRepository;

      @Autowired
      private UploadSessionRepository uploadSessionRepository;

      @Autowired
      private UserRepository userRepository;

      @MockBean
      private S3StorageService s3StorageService;

      private User testUser;

    @BeforeEach
    void setUp() {
        // Create a unique user each time to avoid unique index collisions between tests
        String uniqueEmail = "test+" + UUID.randomUUID() + "@example.com";
        testUser = new User(uniqueEmail, "password123");
        testUser = userRepository.save(testUser);

          // Mock S3 interactions
          when(s3StorageService.getBucketName()).thenReturn("test-bucket");
          when(s3StorageService.initiateMultipartUpload(anyString(), anyString()))
              .thenReturn("test-upload-id-" + UUID.randomUUID());
          when(s3StorageService.generatePresignedUploadUrls(anyString(), anyString(), anyInt()))
              .thenAnswer(invocation -> {
                  int parts = invocation.getArgument(2);
                  return List.of(
                      new InitiateUploadResponse.PresignedPartUrl(1, "https://s3.example.com/part1"),
                      new InitiateUploadResponse.PresignedPartUrl(2, "https://s3.example.com/part2")
                  ).subList(0, Math.min(parts, 2));
              });
          when(s3StorageService.completeMultipartUpload(anyString(), anyString(), anyList()))
              .thenReturn("test-etag-" + UUID.randomUUID());
      }

      @Test
      void shouldCompleteFullUploadFlowSuccessfully() {
          // Step 1: Create upload session
          CreateSessionCommand sessionCommand = new CreateSessionCommand(testUser.getId(), 2);
          UploadSession session = createSessionHandler.handle(sessionCommand);

          assertThat(session).isNotNull();
          assertThat(session.getUserId()).isEqualTo(testUser.getId());

          // Step 2: Initiate upload
          InitiateUploadCommand initiateCommand = new InitiateUploadCommand(
              testUser.getId(),
              "test-photo.jpg",
              "image/jpeg",
              2_000_000L,
              session.getId(),
              List.of("vacation", "beach")
          );

          InitiateUploadResponse initiateResponse = initiateUploadHandler.handle(initiateCommand);

          assertThat(initiateResponse).isNotNull();
          assertThat(initiateResponse.getPhotoId()).isNotNull();
          assertThat(initiateResponse.getMultipartUploadId()).isNull();
          assertThat(initiateResponse.isSinglePartUpload()).isTrue();
          assertThat(initiateResponse.getPresignedUrls()).hasSize(1);

          // Verify photo was created in database
          Photo photo = photoRepository.findById(initiateResponse.getPhotoId()).orElseThrow();
          assertThat(photo.getUploadStatus()).isEqualTo(UploadStatus.INITIATED);
          assertThat(photo.getUserId()).isEqualTo(testUser.getId());
          assertThat(photo.getUploadSessionId()).isEqualTo(session.getId());

          // Step 3: Complete upload
          CompleteUploadCommand completeCommand = new CompleteUploadCommand(
              initiateResponse.getPhotoId(),
              testUser.getId(),
              List.of(new CompleteUploadCommand.PartInfo(1, "etag-1", 2_000_000L))
          );

          completeUploadHandler.handle(completeCommand);

          // Step 4: Verify final state
          Photo completedPhoto = photoRepository.findById(initiateResponse.getPhotoId()).orElseThrow();
          assertThat(completedPhoto.getUploadStatus()).isEqualTo(UploadStatus.COMPLETED);
          assertThat(completedPhoto.getS3Etag()).isNotNull();

          // Verify session was updated
          UploadSession updatedSession = uploadSessionRepository.findById(session.getId()).orElseThrow();
          assertThat(updatedSession.getCompletedPhotos()).isEqualTo(1);
      }

      @Test
      void shouldHandleMultipleConcurrentUploads() throws InterruptedException {
          // Create session for batch upload
          CreateSessionCommand sessionCommand = new CreateSessionCommand(testUser.getId(), 10);
          UploadSession session = createSessionHandler.handle(sessionCommand);

          // Initiate 10 uploads concurrently
          List<Thread> threads = new java.util.ArrayList<>();
          List<UUID> photoIds = new java.util.concurrent.CopyOnWriteArrayList<>();

          for (int i = 0; i < 10; i++) {
              final int index = i;
              Thread thread = new Thread(() -> {
                  InitiateUploadCommand command = new InitiateUploadCommand(
                      testUser.getId(),
                      "photo-" + index + ".jpg",
                      "image/jpeg",
                      2_000_000L,
                      session.getId(),
                      null
                  );

                  InitiateUploadResponse response = initiateUploadHandler.handle(command);
                  photoIds.add(response.getPhotoId());
              });
              threads.add(thread);
              thread.start();
          }

          // Wait for all threads to complete
          for (Thread thread : threads) {
              thread.join();
          }

          // Verify all photos were created
          assertThat(photoIds).hasSize(10);

          List<Photo> photos = photoRepository.findByUploadSessionId(session.getId());
          assertThat(photos).hasSize(10);
          assertThat(photos).allMatch(p -> p.getUploadStatus() == UploadStatus.INITIATED);
      }

      @Test
      void shouldTrackSessionProgress() {
          // Create session
          CreateSessionCommand sessionCommand = new CreateSessionCommand(testUser.getId(), 3);
          UploadSession session = createSessionHandler.handle(sessionCommand);

          // Upload and complete 3 photos
          for (int i = 0; i < 3; i++) {
              InitiateUploadCommand initiateCommand = new InitiateUploadCommand(
                  testUser.getId(),
                  "photo-" + i + ".jpg",
                  "image/jpeg",
                  2_000_000L,
                  session.getId(),
                  null
              );

              InitiateUploadResponse response = initiateUploadHandler.handle(initiateCommand);

              CompleteUploadCommand completeCommand = new CompleteUploadCommand(
                  response.getPhotoId(),
                  testUser.getId(),
                  List.of(new CompleteUploadCommand.PartInfo(1, "etag-" + i, 2_000_000L))
              );

              completeUploadHandler.handle(completeCommand);
          }

          // Verify session completion
          UploadSession completedSession = uploadSessionRepository.findById(session.getId()).orElseThrow();
          assertThat(completedSession.getTotalPhotos()).isEqualTo(3);
          assertThat(completedSession.getCompletedPhotos()).isEqualTo(3);
          assertThat(completedSession.getStatus().name()).isEqualTo("COMPLETED");
      }

      @Test
      void shouldPreventUnauthorizedAccess() {
          // Create photo for user 1
          InitiateUploadCommand initiateCommand = new InitiateUploadCommand(
              testUser.getId(),
              "photo.jpg",
              "image/jpeg",
              2_000_000L,
              null,
              null
          );

          InitiateUploadResponse response = initiateUploadHandler.handle(initiateCommand);

          // Try to complete with different user
          UUID differentUserId = UUID.randomUUID();
          CompleteUploadCommand completeCommand = new CompleteUploadCommand(
              response.getPhotoId(),
              differentUserId,
              List.of(new CompleteUploadCommand.PartInfo(1, "etag", 2_000_000L))
          );

          // Should throw SecurityException
          org.junit.jupiter.api.Assertions.assertThrows(
              SecurityException.class,
              () -> completeUploadHandler.handle(completeCommand)
          );
      }
  }
