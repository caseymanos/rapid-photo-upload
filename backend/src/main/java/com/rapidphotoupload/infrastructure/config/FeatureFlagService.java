package com.rapidphotoupload.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper that exposes typed access to configured feature flags.
 */
@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagProperties properties;

    public boolean isEnabled(String key) {
        return properties.isEnabled(key);
    }
}


