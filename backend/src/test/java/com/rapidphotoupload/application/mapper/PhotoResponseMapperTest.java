package com.rapidphotoupload.application.mapper;

import com.rapidphotoupload.application.dto.PhotoResponse;
import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.domain.model.PhotoMetadata;
import com.rapidphotoupload.domain.model.UploadStatus;
import com.rapidphotoupload.infrastructure.cdn.ImageCdnProperties;
import com.rapidphotoupload.infrastructure.cdn.ImageCdnUrlService;
import com.rapidphotoupload.infrastructure.config.FeatureFlagProperties;
import com.rapidphotoupload.infrastructure.config.FeatureFlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoResponseMapperTest {

    private ImageCdnProperties imageCdnProperties;
    private FeatureFlagProperties featureFlagProperties;

    @BeforeEach
    void setUp() {
        imageCdnProperties = new ImageCdnProperties();
        imageCdnProperties.setEnabled(true);
        imageCdnProperties.setBaseUrl("https://cdn.example.com");
        imageCdnProperties.setPathPrefix("/images");
        imageCdnProperties.setDefaultFormat("webp");
        imageCdnProperties.setAppendCacheBustingToken(false);

        featureFlagProperties = new FeatureFlagProperties();
    }

    @Test
    void shouldFallbackToStoredThumbnailWhenFlagDisabled() {
        featureFlagProperties.getValues().put("image-cdn", false);
        FeatureFlagService featureFlagService = new FeatureFlagService(featureFlagProperties);
        ImageCdnUrlService cdnUrlService = new ImageCdnUrlService(imageCdnProperties);
        PhotoResponseMapper mapper = new PhotoResponseMapper(cdnUrlService, imageCdnProperties, featureFlagService);

        Photo photo = buildPhoto("uploads/photo.jpg");
        photo.setThumbnailUrl("https://legacy.example.com/thumb.jpg");

        PhotoResponse response = mapper.toResponse(photo, "download-url");

        assertThat(response.getThumbnailUrl()).isEqualTo("https://legacy.example.com/thumb.jpg");
        assertThat(response.getThumbnailVariants()).doesNotContainKey("thumbnail");
    }

    @Test
    void shouldUseCdnUrlsWhenFlagEnabled() {
        featureFlagProperties.getValues().put("image-cdn", true);
        FeatureFlagService featureFlagService = new FeatureFlagService(featureFlagProperties);
        ImageCdnUrlService cdnUrlService = new ImageCdnUrlService(imageCdnProperties);
        PhotoResponseMapper mapper = new PhotoResponseMapper(cdnUrlService, imageCdnProperties, featureFlagService);

        Photo photo = buildPhoto("uploads/user/photo image.jpg");

        PhotoResponse response = mapper.toResponse(photo, "download-url");

        assertThat(response.getThumbnailUrl())
            .isEqualTo("https://cdn.example.com/images/uploads/user/photo%20image.jpg?w=480&q=75&fmt=webp");
        assertThat(response.getThumbnailVariants())
            .containsEntry("thumbnail", response.getThumbnailUrl())
            .containsKey("preview");
    }

    @Test
    void shouldIncludeResponsiveVariantsFromPhoto() {
        featureFlagProperties.getValues().put("image-cdn", false);
        FeatureFlagService featureFlagService = new FeatureFlagService(featureFlagProperties);
        ImageCdnUrlService cdnUrlService = new ImageCdnUrlService(imageCdnProperties);
        PhotoResponseMapper mapper = new PhotoResponseMapper(cdnUrlService, imageCdnProperties, featureFlagService);

        Photo photo = buildPhoto("uploads/responsive/photo.jpg");
        photo.setThumbnailVariants(Map.of(
            "320w", "https://media.example.com/thumb-320.webp",
            "640w", "https://media.example.com/thumb-640.webp",
            "1024w", "https://media.example.com/thumb-1024.webp"
        ));
        photo.setThumbnailUrl("https://media.example.com/thumb-640.webp");
        photo.setThumbnailFallbackUrl("https://media.example.com/thumb-640.jpg");

        PhotoResponse response = mapper.toResponse(photo, null);

        assertThat(response.getThumbnailVariants())
            .containsEntry("320w", "https://media.example.com/thumb-320.webp")
            .containsEntry("640w", "https://media.example.com/thumb-640.webp")
            .containsEntry("1024w", "https://media.example.com/thumb-1024.webp")
            .doesNotContainKey("thumbnail");
        assertThat(response.getThumbnailUrl()).isEqualTo("https://media.example.com/thumb-640.webp");
        assertThat(response.getThumbnailFallbackUrl()).isEqualTo("https://media.example.com/thumb-640.jpg");
    }

    private Photo buildPhoto(String s3Key) {
        return new Photo(
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            s3Key,
            "bucket",
            null,
            "photo.jpg",
            1024L,
            "image/jpeg",
            UploadStatus.COMPLETED,
            null,
            null,
            new PhotoMetadata(),
            null,
            new LinkedHashMap<>(),
            null,
            null,
            null,
            null,
            Instant.now(),
            Instant.now(),
            1L
        );
    }
}


