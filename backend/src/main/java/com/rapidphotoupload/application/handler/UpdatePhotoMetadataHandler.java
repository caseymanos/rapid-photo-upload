package com.rapidphotoupload.application.handler;

import com.rapidphotoupload.application.command.UpdatePhotoMetadataCommand;
import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.domain.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for updating photo metadata.
 * Updates tags and other metadata on existing photos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdatePhotoMetadataHandler {
    
    private final PhotoRepository photoRepository;
    
    @Transactional
    public void handle(UpdatePhotoMetadataCommand command) {
        log.info("Updating metadata for photo {}", command.getPhotoId());
        
        // Retrieve photo
        Photo photo = photoRepository.findById(command.getPhotoId())
            .orElseThrow(() -> new IllegalArgumentException("Photo not found"));
        
        // Verify ownership
        if (!photo.getUserId().equals(command.getUserId())) {
            throw new SecurityException("User does not own this photo");
        }
        
        // Update tags
        if (command.getTags() != null) {
            command.getTags().forEach(photo::addTag);
        }
        
        // Save photo
        photoRepository.save(photo);
        
        log.info("Metadata updated for photo {}", command.getPhotoId());
    }
}
