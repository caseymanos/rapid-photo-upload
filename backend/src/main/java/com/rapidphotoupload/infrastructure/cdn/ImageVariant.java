package com.rapidphotoupload.infrastructure.cdn;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enumeration of supported image variants produced by the CDN.
 */
@Getter
public enum ImageVariant {
    /**
     * Default thumbnail used throughout the gallery grid.
     */
    THUMBNAIL(480, null, 75),

    /**
     * Medium sized preview for lightboxes or detail views.
     */
    PREVIEW(1024, null, 80);

    private final Integer width;
    private final Integer height;
    private final Integer quality;

    ImageVariant(Integer width, Integer height, Integer quality) {
        this.width = width;
        this.height = height;
        this.quality = quality;
    }

    /**
     * Convert the variant metadata into query parameters used by the CDN.
     */
    public Map<String, String> toQueryParameters(String defaultFormat) {
        Map<String, String> params = new LinkedHashMap<>();
        if (width != null) {
            params.put("w", width.toString());
        }
        if (height != null) {
            params.put("h", height.toString());
        }
        if (quality != null) {
            params.put("q", quality.toString());
        }
        if (defaultFormat != null && !defaultFormat.isBlank()) {
            params.put("fmt", defaultFormat);
        }
        return params;
    }
}


