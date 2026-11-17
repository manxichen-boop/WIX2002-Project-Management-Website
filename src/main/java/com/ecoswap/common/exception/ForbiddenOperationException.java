package com.ecoswap.common.exception;

import org.springframework.http.HttpStatus;

/** The caller is authenticated but does not own / may not act on the resource (HTTP 403). */
public class ForbiddenOperationException extends ApiException {
    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
