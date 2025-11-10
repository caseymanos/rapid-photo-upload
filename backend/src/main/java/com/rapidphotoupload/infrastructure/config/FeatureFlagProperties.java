package com.rapidphotoupload.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple configuration holder for boolean feature flags.
 */
@Data
@ConfigurationProperties(prefix = "feature.flags")
public class FeatureFlagProperties {

    private Map<String, Boolean> values = new HashMap<>();

    public boolean isEnabled(String key) {
        return values.getOrDefault(key, Boolean.FALSE);
    }
}


