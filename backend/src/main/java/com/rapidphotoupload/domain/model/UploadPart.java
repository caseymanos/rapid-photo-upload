package com.rapidphotoupload.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UploadPart {
    
    private UUID id;
    private UUID photoId;
    private int partNumber;
    private String etag;
    private long sizeBytes;
    
    public UploadPart(int partNumber, String etag, long sizeBytes) {
        this.partNumber = partNumber;
        this.etag = etag;
        this.sizeBytes = sizeBytes;
    }
}