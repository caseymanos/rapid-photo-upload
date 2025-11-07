package com.rapidphotoupload.shared.exception;

/**
 * Exception thrown when a user attempts an unauthorized action.
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
}
