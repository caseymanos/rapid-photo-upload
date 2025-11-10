package com.rapidphotoupload.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response wrapper for photo queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoPageResponse {

    private List<PhotoResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private boolean hasNext;
}
