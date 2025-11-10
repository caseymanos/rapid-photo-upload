package com.rapidphotoupload.application.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Command representing a request to delete a single photo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeletePhotoCommand {

    private UUID photoId;
    private UUID userId;
}
