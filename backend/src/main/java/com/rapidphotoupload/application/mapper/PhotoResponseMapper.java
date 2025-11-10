package com.rapidphotoupload.application.mapper;

import com.rapidphotoupload.application.dto.PhotoResponse;
import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.infrastructure.cdn.ImageCdnProperties;
import com.rapidphotoupload.infrastructure.cdn.ImageCdnUrlService;
import com.rapidphotoupload.infrastructure.cdn.ImageVariant;
import com.rapidphotoupload.infrastructure.config.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Central place for mapping {@link Photo} aggregates to {@link PhotoResponse} DTOs.
 */
@Component
@RequiredArgsConstructor
public class PhotoResponseMapper {

    private final ImageCdnUrlService imageCdnUrlService;
    private final ImageCdnProperties cdnProperties;
    private final FeatureFlagService featureFlagService;

    public PhotoResponse toResponse(Photo photo, String downloadUrl) {
        boolean rolloutEnabled = imageCdnUrlService.isActive()
            && featureFlagService.isEnabled(cdnProperties.getFeatureFlagKey());

        Map<String, String> variants = new LinkedHashMap<>(photo.getThumbnailVariants());

        Optional<String> cdnThumbnail = rolloutEnabled
            ? imageCdnUrlService.buildUrl(photo.getS3Key(), ImageVariant.THUMBNAIL)
            : Optional.empty();
        cdnThumbnail.ifPresent(url -> variants.put("thumbnail", url));

        Optional<String> cdnPreview = rolloutEnabled
            ? imageCdnUrlService.buildUrl(photo.getS3Key(), ImageVariant.PREVIEW)
            : Optional.empty();
        cdnPreview.ifPresent(url -> variants.put("preview", url));

        String primaryThumbnail = cdnThumbnail
            .orElseGet(() -> resolveExistingThumbnail(photo));

        return new PhotoResponse(
            photo.getId(),
            photo.getUserId(),
            photo.getUploadSessionId(),
            photo.getS3Key(),
            photo.getS3Bucket(),
            photo.getOriginalFilename(),
            photo.getFileSizeBytes(),
            photo.getMimeType(),
            photo.getUploadStatus().name(),
            photo.getMetadata().getTags(),
            primaryThumbnail,
            variants,
            photo.getThumbnailFallbackUrl(),
            photo.getPlaceholderUrl(),
            photo.getPlaceholderFallbackUrl(),
            photo.getPlaceholderBase64(),
            downloadUrl,
            photo.getCreatedAt(),
            photo.getUpdatedAt()
        );
    }

    private String resolveExistingThumbnail(Photo photo) {
        if (StringUtils.hasText(photo.getThumbnailUrl())) {
            return photo.getThumbnailUrl();
        }
        if (StringUtils.hasText(photo.getThumbnailFallbackUrl())) {
            return photo.getThumbnailFallbackUrl();
        }
        return null;
    }
}


