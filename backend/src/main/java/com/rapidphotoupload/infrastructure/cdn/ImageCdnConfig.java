package com.rapidphotoupload.infrastructure.cdn;

import com.rapidphotoupload.infrastructure.config.FeatureFlagProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration entry point for CDN image support and related flags.
 */
@Configuration
@EnableConfigurationProperties({ImageCdnProperties.class, FeatureFlagProperties.class})
public class ImageCdnConfig {
}


