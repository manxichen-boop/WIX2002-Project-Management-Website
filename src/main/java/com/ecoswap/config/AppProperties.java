package com.ecoswap.config;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of the {@code ecoswap.*} settings in application.yml. */
@ConfigurationProperties(prefix = "ecoswap")
public record AppProperties(String baseUrl, String displayTimeZone, Storage storage, Admin admin, Auth auth, Mail mail) {

    public ZoneId zone() {
        return ZoneId.of(displayTimeZone);
    }

    public record Storage(String localDir, long maxImageBytes, int maxImagesPerItem, long maxAttachmentBytes) {}

    public record Admin(String bootstrapEmail, String bootstrapPassword, String bootstrapName) {}

    public record Auth(boolean requireEmailVerification, String allowedEmailDomains, String rememberMeKey,
                       int rememberMeDays, int maxLoginAttempts, int loginLockMinutes) {

        /** Lower-case e-mail domains students may register with; empty means any domain. */
        public List<String> allowedDomains() {
            if (allowedEmailDomains == null || allowedEmailDomains.isBlank()) {
                return List.of();
            }
            return Arrays.stream(allowedEmailDomains.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).map(String::toLowerCase).toList();
        }
    }

    public record Mail(String from) {}
}
