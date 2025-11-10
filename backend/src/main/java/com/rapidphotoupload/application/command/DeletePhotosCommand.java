package com.rapidphotoupload.application.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Command representing a batch photo deletion request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeletePhotosCommand {

    private UUID userId;
    private List<UUID> photoIds;
}
