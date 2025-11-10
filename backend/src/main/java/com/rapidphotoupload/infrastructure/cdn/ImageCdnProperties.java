package com.rapidphotoupload.infrastructure.cdn;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration properties for CDN-based image delivery.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "cdn.images")
public class ImageCdnProperties {

    /**
     * Global toggle that enables CDN URL generation.
     */
    private boolean enabled = false;

    /**
     * Base URL for the CDN distribution, for example https://d123.cloudfront.net.
     */
    private String baseUrl;

    /**
     * Path prefix where optimized images are exposed, defaults to /images.
     */
    private String pathPrefix = "/images";

    /**
     * Preferred default image format (e.g. webp, avif).
     */
    private String defaultFormat = "webp";

    /**
     * Feature flag key to gate rollout of CDN image URLs.
     */
    private String featureFlagKey = "image-cdn";

    /**
     * Whether to append cache-busting signature parameters even when signing is disabled.
     */
    private boolean appendCacheBustingToken = true;

    private final Signing signing = new Signing();

    @Data
    public static class Signing {
        /**
         * Enable CloudFront-style URL signing.
         */
        private boolean enabled = false;

        /**
         * CloudFront key pair identifier associated with the private key.
         */
        private String keyPairId;

        /**
         * RSA private key in PEM format used to sign URLs.
         */
        private String privateKeyPem;

        /**
         * Lifetime of generated signed URLs.
         */
        private Duration urlTtl = Duration.ofMinutes(30);
    }
}


