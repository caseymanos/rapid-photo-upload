package com.rapidphotoupload.infrastructure.cdn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper service that builds CDN URLs (optionally signed) for S3-backed photos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCdnUrlService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final ImageCdnProperties properties;

    private final AtomicBoolean signingInitialized = new AtomicBoolean(false);
    private volatile PrivateKey signingKey;

    public boolean isActive() {
        return properties.isEnabled() && StringUtils.hasText(properties.getBaseUrl());
    }

    /**
     * Generate a CDN URL for the provided S3 key and variant.
     *
     * @param s3Key   S3 object key (relative path inside the bucket)
     * @param variant desired image variant parameters
     * @return optional CDN URL (empty when CDN is disabled or configuration incomplete)
     */
    public Optional<String> buildUrl(String s3Key, ImageVariant variant) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            log.warn("CDN is enabled but baseUrl is not configured");
            return Optional.empty();
        }
        if (!StringUtils.hasText(s3Key)) {
            return Optional.empty();
        }

        String baseUrl = trimTrailingSlash(properties.getBaseUrl());
        String pathPrefix = normalizePathPrefix(properties.getPathPrefix());
        String encodedKey = encodeS3Key(s3Key);

        StringBuilder resourceBuilder = new StringBuilder(baseUrl);
        if (StringUtils.hasText(pathPrefix)) {
            resourceBuilder.append('/').append(pathPrefix);
        }
        resourceBuilder.append('/').append(encodedKey);

        Map<String, String> queryParams = new LinkedHashMap<>(variant.toQueryParameters(properties.getDefaultFormat()));

        if (properties.isAppendCacheBustingToken()) {
            queryParams.putIfAbsent("v", Integer.toUnsignedString(encodedKey.hashCode()));
        }

        String resourceUrl = appendQuery(resourceBuilder.toString(), queryParams);

        if (!properties.getSigning().isEnabled()) {
            return Optional.of(resourceUrl);
        }

        return Optional.of(signUrl(resourceUrl));
    }

    private String signUrl(String resourceUrl) {
        if (!StringUtils.hasText(properties.getSigning().getKeyPairId())) {
            log.warn("CDN signing enabled but keyPairId not configured; returning unsigned URL");
            return resourceUrl;
        }

        PrivateKey key = loadSigningKey();
        if (key == null) {
            log.warn("CDN signing enabled but private key could not be loaded; returning unsigned URL");
            return resourceUrl;
        }

        Duration ttl = Optional.ofNullable(properties.getSigning().getUrlTtl())
            .filter(duration -> !duration.isNegative() && !duration.isZero())
            .orElse(Duration.ofMinutes(30));

        long expiresAt = Instant.now().plus(ttl).getEpochSecond();

        String policy = buildPolicy(resourceUrl, expiresAt);
        byte[] signatureBytes = rsaSign(policy.getBytes(StandardCharsets.UTF_8), key);

        String encodedPolicy = URL_ENCODER.encodeToString(policy.getBytes(StandardCharsets.UTF_8));
        String encodedSignature = URL_ENCODER.encodeToString(signatureBytes);

        char joinChar = resourceUrl.contains("?") ? '&' : '?';

        return new StringBuilder(resourceUrl)
            .append(joinChar).append("Policy=").append(encodedPolicy)
            .append("&Signature=").append(encodedSignature)
            .append("&Key-Pair-Id=").append(properties.getSigning().getKeyPairId())
            .toString();
    }

    private PrivateKey loadSigningKey() {
        if (!signingInitialized.get()) {
            synchronized (this) {
                if (!signingInitialized.get()) {
                    signingKey = parsePrivateKey(properties.getSigning().getPrivateKeyPem());
                    signingInitialized.set(true);
                }
            }
        }
        return signingKey;
    }

    private PrivateKey parsePrivateKey(String pem) {
        if (!StringUtils.hasText(pem)) {
            return null;
        }
        try {
            String sanitized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(sanitized);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception ex) {
            log.error("Failed to parse CDN signing private key", ex);
            return null;
        }
    }

    private byte[] rsaSign(byte[] payload, PrivateKey key) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(payload);
            return signature.sign();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign CDN policy", ex);
        }
    }

    private String buildPolicy(String resourceUrl, long expiresAt) {
        return String.format(
            "{\"Statement\":[{\"Resource\":\"%s\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":%d}}}]}",
            resourceUrl,
            expiresAt
        );
    }

    private String appendQuery(String base, Map<String, String> params) {
        if (params.isEmpty()) {
            return base;
        }
        StringBuilder builder = new StringBuilder(base);
        builder.append(base.contains("?") ? '&' : '?');
        java.util.StringJoiner joiner = new java.util.StringJoiner("&");
        params.forEach((key, value) -> joiner.add(key + "=" + urlEncode(value)));
        builder.append(joiner);
        return builder.toString();
    }

    private String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        String encoded = java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        return encoded.replace("+", "%20");
    }

    private String encodeS3Key(String s3Key) {
        String[] segments = s3Key.split("/");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            String encoded = urlEncode(segments[i]);
            builder.append(encoded);
            if (i < segments.length - 1) {
                builder.append('/');
            }
        }
        return builder.toString();
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String normalizePathPrefix(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value;
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}


