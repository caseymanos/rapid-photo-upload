package com.rapidphotoupload.infrastructure.media;

import com.rapidphotoupload.domain.model.Photo;
import com.rapidphotoupload.domain.model.UploadStatus;
import com.rapidphotoupload.domain.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.ImageFilter;
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
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoDerivativeService {

    private static final List<VariantSpec> THUMBNAIL_VARIANTS = List.of(
        new VariantSpec("320w", 320),
        new VariantSpec("640w", 640),
        new VariantSpec("1024w", 1024)
    );
    private static final String DEFAULT_VARIANT_KEY = "640w";
    private static final int PLACEHOLDER_EDGE = 32;
    private static final float THUMBNAIL_WEBP_QUALITY = 0.82f;
    private static final float THUMBNAIL_JPEG_QUALITY = 0.85f;
    private static final float PLACEHOLDER_WEBP_QUALITY = 0.6f;
    private static final float PLACEHOLDER_JPEG_QUALITY = 0.65f;
    private static final int PLACEHOLDER_BLUR_ITERATIONS = 2;
    private static final ImageFilter PLACEHOLDER_BLUR_FILTER = new BoxBlurFilter(PLACEHOLDER_BLUR_ITERATIONS);

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

    public void generateDerivatives(UUID photoId) {
        generateDerivativesInternal(photoId);
    }

    @Async("thumbnailExecutor")
    public void generateDerivativesAsync(UUID photoId) {
        generateDerivativesInternal(photoId);
    }

    private void generateDerivativesInternal(UUID photoId) {
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

        Map<String, String> variantUrls = new LinkedHashMap<>();
        bundle.thumbnailVariants().forEach((descriptor, variant) -> {
            uploadDerivative(photo, variant.webpKey(), variant.webpBytes(), "image/webp");
            uploadDerivative(photo, variant.jpegKey(), variant.jpegBytes(), "image/jpeg");
            variantUrls.put(descriptor, buildPublicUrl(photo, variant.webpKey()));
        });
        uploadDerivative(photo, bundle.placeholderWebpKey(), bundle.placeholderWebpBytes(), "image/webp");
        uploadDerivative(photo, bundle.placeholderJpegKey(), bundle.placeholderJpegBytes(), "image/jpeg");

        ThumbnailVariant defaultVariant = resolveDefaultVariant(bundle);

        photo.setThumbnailVariants(variantUrls);
        if (defaultVariant != null) {
            photo.setThumbnailUrl(buildPublicUrl(photo, defaultVariant.webpKey()));
            photo.setThumbnailFallbackUrl(buildPublicUrl(photo, defaultVariant.jpegKey()));
        }
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

        String placeholderWebpKey = prefix + photoId + "-small.webp";
        String placeholderJpegKey = prefix + photoId + "-small.jpg";
        Map<String, ThumbnailVariant> variants = new LinkedHashMap<>();

        for (VariantSpec spec : THUMBNAIL_VARIANTS) {
            String baseKey = prefix + photoId + "-" + spec.key();
            String webpKey = baseKey + ".webp";
            String jpegKey = baseKey + ".jpg";

            byte[] webpBytes = renderDerivative(original, spec.edge(), false, "webp", THUMBNAIL_WEBP_QUALITY);
            byte[] jpegBytes = renderDerivative(original, spec.edge(), false, "jpg", THUMBNAIL_JPEG_QUALITY);

            variants.put(spec.key(), new ThumbnailVariant(webpKey, webpBytes, jpegKey, jpegBytes));
        }

        byte[] placeholderWebpBytes = renderDerivative(original, PLACEHOLDER_EDGE, true, "webp", PLACEHOLDER_WEBP_QUALITY);
        byte[] placeholderJpegBytes = renderDerivative(original, PLACEHOLDER_EDGE, true, "jpg", PLACEHOLDER_JPEG_QUALITY);

        String base64 = Base64.getEncoder().encodeToString(placeholderWebpBytes);
        String dataUri = "data:image/webp;base64," + base64;

        return new DerivativeBundle(
            variants,
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
                builder.addFilter(PLACEHOLDER_BLUR_FILTER);
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
        Map<String, String> variants = photo.getThumbnailVariants();
        boolean hasVariants = variants != null
            && THUMBNAIL_VARIANTS.stream().allMatch(spec -> isSet(variants.get(spec.key())));

        return hasVariants
            && isSet(photo.getThumbnailUrl())
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

    private ThumbnailVariant resolveDefaultVariant(DerivativeBundle bundle) {
        if (bundle.thumbnailVariants().isEmpty()) {
            return null;
        }
        ThumbnailVariant variant = bundle.thumbnailVariants().get(DEFAULT_VARIANT_KEY);
        if (variant != null) {
            return variant;
        }
        return bundle.thumbnailVariants().values().iterator().next();
    }

    private record DerivativeBundle(
        Map<String, ThumbnailVariant> thumbnailVariants,
        String placeholderWebpKey,
        byte[] placeholderWebpBytes,
        String placeholderJpegKey,
        byte[] placeholderJpegBytes,
        String placeholderDataUri
    ) {}

    private record ThumbnailVariant(
        String webpKey,
        byte[] webpBytes,
        String jpegKey,
        byte[] jpegBytes
    ) {}

    private record VariantSpec(
        String key,
        int edge
    ) {}

    private static final class BoxBlurFilter implements ImageFilter {

        private final int iterations;
        private final Kernel kernel;

        private BoxBlurFilter(int iterations) {
            this.iterations = Math.max(1, iterations);
            float[] data = new float[9];
            Arrays.fill(data, 1f / 9f);
            this.kernel = new Kernel(3, 3, data);
        }

        @Override
        public BufferedImage apply(BufferedImage img) {
            BufferedImage current = img;
            for (int i = 0; i < iterations; i++) {
                ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
                current = op.filter(current, null);
            }
            return current;
        }
    }

    private static class UnsupportedImageFormatException extends RuntimeException {
        UnsupportedImageFormatException(String message) {
            super(message);
        }
    }
}
