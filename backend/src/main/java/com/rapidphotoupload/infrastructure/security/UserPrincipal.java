package com.rapidphotoupload.infrastructure.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Represents the authenticated user principal.
 * Contains user ID and email for the current security context.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal {
    
    private UUID userId;
    private String email;
}
