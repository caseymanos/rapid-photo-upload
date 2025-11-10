package com.rapidphotoupload.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.UUID;

/**
 * Validates Supabase JWT tokens.
 */
@Slf4j
@Component
public class SupabaseJwtValidator {
    
    @Value("${security.supabase.jwt-secret}")
    private String supabaseJwtSecret;
    
    /**
     * Validate Supabase JWT token.
     * @param token the JWT token from Supabase Auth
     * @return true if valid
     */
    public boolean validateSupabaseToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Supabase token validation failed", e);
            return false;
        }
    }
    
    /**
     * Extract user ID from Supabase token.
     * Supabase tokens have 'sub' claim with user UUID.
     */
    public UUID extractUserId(String token) {
        Claims claims = extractClaims(token);
        String sub = claims.getSubject();
        return UUID.fromString(sub);
    }
    
    /**
     * Extract email from Supabase token.
     */
    public String extractEmail(String token) {
        Claims claims = extractClaims(token);
        return claims.get("email", String.class);
    }
    
    private Claims extractClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = supabaseJwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

