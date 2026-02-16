package com.platform.application.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom exception for application-service business errors.
 * Carries an errorCode for frontend-friendly error handling.
 */
@Getter
public class ApplicationException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public ApplicationException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ApplicationException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
