package com.ecoswap.common.exception;

import org.springframework.http.HttpStatus;

/** Input that passes bean validation but breaks a rule checked in a service (HTTP 400). */
public class InvalidInputException extends ApiException {

    private final String field;

    public InvalidInputException(String field, String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
        this.field = field;
    }

    public String getField() { return field; }
}
