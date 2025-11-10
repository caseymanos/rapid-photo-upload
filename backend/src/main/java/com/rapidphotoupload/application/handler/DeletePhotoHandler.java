package com.rapidphotoupload.application.handler;

import com.rapidphotoupload.application.command.DeletePhotoCommand;
import com.rapidphotoupload.application.command.DeletePhotosCommand;
import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.domain.repository.PhotoRepository;
import com.rapidphotoupload.infrastructure.storage.S3StorageService;
import com.rapidphotoupload.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Handler responsible for deleting photo assets from both S3 and the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeletePhotoHandler {

    private final PhotoRepository photoRepository;
    private final S3StorageService s3StorageService;

    @Transactional
    public void handle(DeletePhotoCommand command) {
        Photo photo = photoRepository.findById(command.getPhotoId())
            .orElseThrow(() -> new ResourceNotFoundException("Photo not found"));
        deletePhoto(photo, command.getUserId());
    }

    @Transactional
    public void handle(DeletePhotosCommand command) {
        if (command.getPhotoIds() == null || command.getPhotoIds().isEmpty()) {
            return;
        }

        for (UUID photoId : command.getPhotoIds()) {
            try {
                handle(new DeletePhotoCommand(photoId, command.getUserId()));
            } catch (ResourceNotFoundException | SecurityException ex) {
                log.warn("Skipping photo {} during batch delete: {}", photoId, ex.getMessage());
            }
        }
    }

    @Transactional
    public int deleteAllForUser(UUID userId) {
        List<Photo> photos = photoRepository.findByUserId(userId);
        int deleted = 0;
        for (Photo photo : photos) {
            deletePhoto(photo, userId);
            deleted++;
        }
        return deleted;
    }

    private void deletePhoto(Photo photo, UUID userId) {
        if (!photo.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this photo");
        }

        try {
            s3StorageService.deleteObject(photo.getS3Key());
            log.info("Deleted S3 object: {}", photo.getS3Key());
        } catch (Exception ex) {
            log.error("Failed to delete S3 object {}. Proceeding with DB cleanup.", photo.getS3Key(), ex);
        }

        photoRepository.deleteById(photo.getId());
        log.info("Deleted photo {} for user {}", photo.getId(), userId);
    }
}
