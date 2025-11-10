package com.rapidphotoupload.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for User.
 * References auth.users(id) from Supabase Auth.
 * Password is managed by Supabase Auth, not stored here.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    
    @Id
    private UUID id;  // References auth.users(id)
    
    @Column(nullable = false, unique = true)
    private String email;
    
    // Password hash removed - managed by Supabase Auth
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
