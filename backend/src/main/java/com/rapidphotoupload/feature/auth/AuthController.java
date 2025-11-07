package com.rapidphotoupload.feature.auth;

import com.rapidphotoupload.application.command.RegisterUserCommand;
import com.rapidphotoupload.application.dto.AuthResponse;
import com.rapidphotoupload.application.dto.LoginRequest;
import com.rapidphotoupload.application.dto.RegisterRequest;
import com.rapidphotoupload.domain.model.User;
import com.rapidphotoupload.domain.repository.UserRepository;
import com.rapidphotoupload.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations (Vertical Slice).
 * Handles user registration and login.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    @Value("${security.jwt.expiration-ms}")
    private long jwtExpirationMs;
    
    /**
     * Register a new user.
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());
        
        // Check if user exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        // Create user
        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getEmail(), passwordHash);
        
        User savedUser = userRepository.save(user);
        
        // Generate JWT
        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail());
        
        AuthResponse response = new AuthResponse(
            token,
            savedUser.getId(),
            savedUser.getEmail(),
            jwtExpirationMs / 1000 // Convert to seconds
        );
        
        log.info("User registered successfully: {}", savedUser.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Login a user.
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());
        
        // Find user
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        
        // Generate JWT
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        
        AuthResponse response = new AuthResponse(
            token,
            user.getId(),
            user.getEmail(),
            jwtExpirationMs / 1000 // Convert to seconds
        );
        
        log.info("User logged in successfully: {}", user.getId());
        
        return ResponseEntity.ok(response);
    }
}
