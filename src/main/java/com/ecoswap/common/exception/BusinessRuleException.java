package com.ecoswap.common.exception;

import org.springframework.http.HttpStatus;

/** A request that is well-formed but not allowed in the current state (HTTP 409). */
public class BusinessRuleException extends ApiException {
    public BusinessRuleException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
