package com.platform.user.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom exception for authentication/authorization failures.
 * Mapped to HTTP 401/403 by GlobalExceptionHandler.
 */
public class AuthException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public AuthException(String message) {
        super(message);
        this.status = HttpStatus.UNAUTHORIZED;
        this.errorCode = "AUTH_ERROR";
    }

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = "AUTH_ERROR";
    }

    public AuthException(String message, String errorCode) {
        super(message);
        this.status = HttpStatus.UNAUTHORIZED;
        this.errorCode = errorCode;
    }

    public AuthException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
