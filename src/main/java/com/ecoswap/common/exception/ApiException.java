package com.ecoswap.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected business failures. Each carries an HTTP status and a stable,
 * machine-readable code that the frontend can react to (e.g. ACCOUNT_PENDING).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
