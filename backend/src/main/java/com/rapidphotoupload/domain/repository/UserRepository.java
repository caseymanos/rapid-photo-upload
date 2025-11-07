package com.rapidphotoupload.domain.repository;

import com.rapidphotoupload.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User aggregate.
 * Defines the contract for User persistence operations.
 */
public interface UserRepository {
    
    /**
     * Find a user by their unique identifier.
     * @param userId the user ID
     * @return Optional containing the user if found
     */
    Optional<User> findById(UUID userId);
    
    /**
     * Find a user by their email address.
     * @param email the user's email
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Save or update a user.
     * @param user the user to save
     * @return the saved user
     */
    User save(User user);
    
    /**
     * Check if a user with the given email exists.
     * @param email the email to check
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);
}
