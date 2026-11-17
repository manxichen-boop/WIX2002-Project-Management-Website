package com.ecoswap.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Entities store UTC LocalDateTime; DTOs expose Instant so JSON carries an explicit 'Z'. */
public final class Times {

    private Times() {}

    public static Instant utc(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
