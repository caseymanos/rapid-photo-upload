package com.rapidphotoupload.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * JWT authentication filter.
 * Intercepts requests and validates JWT tokens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final SupabaseJwtValidator supabaseJwtValidator;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Try Supabase token first
                if (supabaseJwtValidator.validateSupabaseToken(jwt)) {
                    UUID userId = supabaseJwtValidator.extractUserId(jwt);
                    String email = supabaseJwtValidator.extractEmail(jwt);
                    
                    if (email != null) {
                        UserPrincipal principal = new UserPrincipal(userId, email);
                        
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                new ArrayList<>()
                            );
                        
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } else {
                    // Fallback to custom JWT
                    try {
                        String email = jwtService.extractEmail(jwt);
                        UUID userId = jwtService.extractUserId(jwt);
                        
                        if (email != null && jwtService.validateToken(jwt, email)) {
                            UserPrincipal principal = new UserPrincipal(userId, email);
                            
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    new ArrayList<>()
                                );
                            
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    } catch (Exception e) {
                        log.debug("Custom JWT validation failed, token may be Supabase-only", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Token validation error", e);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
