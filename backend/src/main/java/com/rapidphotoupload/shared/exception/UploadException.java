package com.rapidphotoupload.shared.exception;

/**
 * Exception thrown when an upload operation fails.
 */
public class UploadException extends RuntimeException {
    
    public UploadException(String message) {
        super(message);
    }
    
    public UploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
