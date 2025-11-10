package com.rapidphotoupload.infrastructure.cdn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ImageCdnUrlServiceTest {

    private ImageCdnProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ImageCdnProperties();
        properties.setAppendCacheBustingToken(false);
        properties.setBaseUrl("https://cdn.example.com");
        properties.setEnabled(true);
        properties.setPathPrefix("/images");
        properties.setDefaultFormat("webp");
    }

    @Test
    void shouldReturnEmptyWhenDisabled() {
        properties.setEnabled(false);
        ImageCdnUrlService service = new ImageCdnUrlService(properties);

        Optional<String> url = service.buildUrl("uploads/test/photo.jpg", ImageVariant.THUMBNAIL);

        assertThat(url).isEmpty();
    }

    @Test
    void shouldBuildUnsignedUrlWithVariantParameters() {
        ImageCdnUrlService service = new ImageCdnUrlService(properties);

        Optional<String> url = service.buildUrl("uploads/user 1/photo 10.jpg", ImageVariant.THUMBNAIL);

        assertThat(url).isPresent();
        assertThat(url.get())
            .isEqualTo("https://cdn.example.com/images/uploads/user%201/photo%2010.jpg?w=480&q=75&fmt=webp");
    }

    @Test
    void shouldIncludeSignatureParametersWhenSigningEnabled() throws Exception {
        ImageCdnProperties.Signing signing = properties.getSigning();
        signing.setEnabled(true);
        signing.setKeyPairId("test-key");
        signing.setPrivateKeyPem(generatePrivateKeyPem());

        ImageCdnUrlService service = new ImageCdnUrlService(properties);

        Optional<String> url = service.buildUrl("uploads/user/photo.jpg", ImageVariant.PREVIEW);

        assertThat(url).isPresent();
        assertThat(url.get()).contains("Policy=");
        assertThat(url.get()).contains("Signature=");
        assertThat(url.get()).contains("Key-Pair-Id=test-key");
    }

    private String generatePrivateKeyPem() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        String base64 = java.util.Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----";
    }
}


