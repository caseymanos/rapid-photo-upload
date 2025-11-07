package com.rapidphotoupload.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * User aggregate root.
 * Represents a user in the system with authentication credentials.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    private UUID id;
    private String email;
    private String passwordHash;
    private Instant createdAt;
    private Instant updatedAt;
    
    /**
     * Create a new user with the given email and password hash.
     * @param email the user's email
     * @param passwordHash the hashed password
     */
    public User(String email, String passwordHash) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Update the user's password.
     * @param newPasswordHash the new hashed password
     */
    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Update the user's email.
     * @param newEmail the new email
     */
    public void updateEmail(String newEmail) {
        this.email = newEmail;
        this.updatedAt = Instant.now();
    }
}
