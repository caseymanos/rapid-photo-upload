package com.rapidphotoupload.infrastructure.media;

import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.domain.model.UploadStatus;
import com.rapidphotoupload.domain.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.GaussianBlur;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoDerivativeService {

    private static final int MEDIUM_EDGE = 600;
    private static final int PLACEHOLDER_EDGE = 32;
    private static final float MEDIUM_WEBP_QUALITY = 0.82f;
    private static final float MEDIUM_JPEG_QUALITY = 0.85f;
    private static final float PLACEHOLDER_WEBP_QUALITY = 0.6f;
    private static final float PLACEHOLDER_JPEG_QUALITY = 0.65f;
    private static final double PLACEHOLDER_BLUR_RADIUS = 12.0;

    static {
        // Ensure ImageIO discovers plugins like WebP once at startup.
        ImageIO.scanForPlugins();
        ImageIO.setUseCache(false);
    }

    private final PhotoRepository photoRepository;
    private final S3Client s3Client;

    @Value("${aws.s3.thumbnails.prefix:thumbnails/}")
    private String thumbnailPrefix;

    @Value("${aws.s3.public-base-url:}")
    private String publicBaseUrl;

    @Async("thumbnailExecutor")
    public void generateDerivativesAsync(UUID photoId) {
        long start = System.currentTimeMillis();
        Optional<Photo> photoOpt = photoRepository.findById(photoId);

        if (photoOpt.isEmpty()) {
            log.warn("Skipped derivative generation; photo {} not found", photoId);
            return;
        }

        Photo photo = photoOpt.get();

        if (photo.getUploadStatus() != UploadStatus.COMPLETED) {
            log.debug("Skipping derivative generation for photo {} - upload status {}", photoId, photo.getUploadStatus());
            return;
        }

        if (hasAllDerivatives(photo)) {
            log.debug("Derivatives already exist for photo {}, skipping regeneration", photoId);
            return;
        }

        try {
            processPhoto(photo);
            log.info("Generated derivatives for photo {} in {} ms", photoId, System.currentTimeMillis() - start);
        } catch (UnsupportedImageFormatException e) {
            log.warn("Unsupported image format for photo {}: {}", photoId, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to generate derivatives for photo {}", photoId, e);
        }
    }

    private void processPhoto(Photo photo) throws IOException {
        byte[] originalBytes = downloadOriginal(photo);
        if (originalBytes == null || originalBytes.length == 0) {
            throw new IOException("Original object is empty or missing");
        }

        BufferedImage original;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes)) {
            original = ImageIO.read(inputStream);
        }

        if (original == null) {
            throw new UnsupportedImageFormatException("Image format not supported by ImageIO");
        }

        DerivativeBundle bundle = createDerivatives(photo.getId(), original);

        uploadDerivative(photo, bundle.mediumWebpKey(), bundle.mediumWebpBytes(), "image/webp");
        uploadDerivative(photo, bundle.mediumJpegKey(), bundle.mediumJpegBytes(), "image/jpeg");
        uploadDerivative(photo, bundle.placeholderWebpKey(), bundle.placeholderWebpBytes(), "image/webp");
        uploadDerivative(photo, bundle.placeholderJpegKey(), bundle.placeholderJpegBytes(), "image/jpeg");

        photo.setThumbnailUrl(buildPublicUrl(photo, bundle.mediumWebpKey()));
        photo.setThumbnailFallbackUrl(buildPublicUrl(photo, bundle.mediumJpegKey()));
        photo.setPlaceholderUrl(buildPublicUrl(photo, bundle.placeholderWebpKey()));
        photo.setPlaceholderFallbackUrl(buildPublicUrl(photo, bundle.placeholderJpegKey()));
        photo.setPlaceholderBase64(bundle.placeholderDataUri());

        photoRepository.save(photo);
    }

    private byte[] downloadOriginal(Photo photo) {
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(photo.getS3Bucket())
            .key(photo.getS3Key())
            .build();

        try {
            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObject(request, ResponseTransformer.toBytes());
            return responseBytes.asByteArray();
        } catch (S3Exception e) {
            log.error("Unable to download original photo {} from S3: {}", photo.getId(), e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error downloading photo {} from S3", photo.getId(), e);
        }
        return null;
    }

    private DerivativeBundle createDerivatives(UUID photoId, BufferedImage original) throws IOException {
        String prefix = normalizedPrefix();

        String mediumWebpKey = prefix + photoId + "-medium.webp";
        String mediumJpegKey = prefix + photoId + "-medium.jpg";
        String placeholderWebpKey = prefix + photoId + "-small.webp";
        String placeholderJpegKey = prefix + photoId + "-small.jpg";

        byte[] mediumWebpBytes = renderDerivative(original, MEDIUM_EDGE, false, "webp", MEDIUM_WEBP_QUALITY);
        byte[] mediumJpegBytes = renderDerivative(original, MEDIUM_EDGE, false, "jpg", MEDIUM_JPEG_QUALITY);
        byte[] placeholderWebpBytes = renderDerivative(original, PLACEHOLDER_EDGE, true, "webp", PLACEHOLDER_WEBP_QUALITY);
        byte[] placeholderJpegBytes = renderDerivative(original, PLACEHOLDER_EDGE, true, "jpg", PLACEHOLDER_JPEG_QUALITY);

        String base64 = Base64.getEncoder().encodeToString(placeholderWebpBytes);
        String dataUri = "data:image/webp;base64," + base64;

        return new DerivativeBundle(
            mediumWebpKey,
            mediumWebpBytes,
            mediumJpegKey,
            mediumJpegBytes,
            placeholderWebpKey,
            placeholderWebpBytes,
            placeholderJpegKey,
            placeholderJpegBytes,
            dataUri
        );
    }

    private byte[] renderDerivative(BufferedImage original, int maxEdge, boolean applyBlur, String format, float quality) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(original)
                .size(maxEdge, maxEdge)
                .keepAspectRatio(true)
                .outputFormat(format)
                .outputQuality(quality);

            if (applyBlur) {
                builder.addFilter(new GaussianBlur(PLACEHOLDER_BLUR_RADIUS));
            }

            builder.toOutputStream(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void uploadDerivative(Photo photo, String key, byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) {
            return;
        }

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(photo.getS3Bucket())
            .key(key)
            .contentType(contentType)
            .cacheControl("public, max-age=" + Duration.ofDays(365).toSeconds())
            .contentLength((long) bytes.length)
            .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
        } catch (S3Exception e) {
            log.error("Failed to upload derivative {} for photo {}: {}", key, photo.getId(), e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
            throw e;
        }
    }

    private boolean hasAllDerivatives(Photo photo) {
        return isSet(photo.getThumbnailUrl())
            && isSet(photo.getThumbnailFallbackUrl())
            && isSet(photo.getPlaceholderUrl())
            && isSet(photo.getPlaceholderFallbackUrl())
            && isSet(photo.getPlaceholderBase64());
    }

    private boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizedPrefix() {
        if (thumbnailPrefix == null || thumbnailPrefix.isBlank()) {
            return "thumbnails/";
        }
        return thumbnailPrefix.endsWith("/") ? thumbnailPrefix : thumbnailPrefix + "/";
    }

    private String buildPublicUrl(Photo photo, String key) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            if (publicBaseUrl.contains("{bucket}")) {
                return appendPath(publicBaseUrl.replace("{bucket}", photo.getS3Bucket()), key);
            }
            if (publicBaseUrl.contains("%s")) {
                return appendPath(String.format(publicBaseUrl, photo.getS3Bucket()), key);
            }
            return appendPath(publicBaseUrl, key);
        }
        return "https://" + photo.getS3Bucket() + ".s3.amazonaws.com/" + key;
    }

    private String appendPath(String base, String key) {
        if (base.endsWith("/")) {
            return base + key;
        }
        return base + "/" + key;
    }

    private record DerivativeBundle(
        String mediumWebpKey,
        byte[] mediumWebpBytes,
        String mediumJpegKey,
        byte[] mediumJpegBytes,
        String placeholderWebpKey,
        byte[] placeholderWebpBytes,
        String placeholderJpegKey,
        byte[] placeholderJpegBytes,
        String placeholderDataUri
    ) {}

    private static class UnsupportedImageFormatException extends RuntimeException {
        UnsupportedImageFormatException(String message) {
            super(message);
        }
    }
}
