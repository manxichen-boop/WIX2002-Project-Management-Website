package com.ecoswap.common.util;

/** Small helpers for normalising user-entered text before it is stored. */
public final class Texts {

    private Texts() {}

    /** Trims and returns null for blank strings, so optional fields are stored as NULL. */
    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String normaliseEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(java.util.Locale.ROOT);
    }

    /** Escapes LIKE wildcards so user search text is matched literally. */
    public static String likePattern(String query) {
        String escaped = query.strip().toLowerCase(java.util.Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
