package com.ecoswap.common.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Slugs {

    private Slugs() {}

    public static String of(String text) {
        String ascii = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "category" : slug;
    }
}
