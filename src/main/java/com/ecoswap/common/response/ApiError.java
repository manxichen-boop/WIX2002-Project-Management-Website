package com.ecoswap.common.response;

import java.time.Instant;
import java.util.List;

/** Uniform error body for every /api endpoint. */
public record ApiError(int status, String code, String message, List<FieldError> fieldErrors, Instant timestamp) {

    public record FieldError(String field, String message) {}

    public static ApiError of(int status, String code, String message) {
        return new ApiError(status, code, message, List.of(), Instant.now());
    }

    public static ApiError of(int status, String code, String message, List<FieldError> fieldErrors) {
        return new ApiError(status, code, message, fieldErrors, Instant.now());
    }
}
