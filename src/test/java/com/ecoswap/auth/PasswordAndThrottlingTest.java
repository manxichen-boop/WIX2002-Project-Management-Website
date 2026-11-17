package com.ecoswap.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecoswap.auth.service.LoginAttemptService;
import com.ecoswap.common.util.TokenGenerator;
import com.ecoswap.common.validation.StrongPasswordValidator;
import com.ecoswap.config.AppProperties;
import org.junit.jupiter.api.Test;

/** Password policy, login throttling and token generation. */
class PasswordAndThrottlingTest {

    @Test
    void passwordPolicy() {
        assertThat(StrongPasswordValidator.isStrong("Swap2026x")).isTrue();
        assertThat(StrongPasswordValidator.isStrong("short1")).isFalse();
        assertThat(StrongPasswordValidator.isStrong("onlyletters")).isFalse();
        assertThat(StrongPasswordValidator.isStrong("12345678")).isFalse();
        assertThat(StrongPasswordValidator.isStrong("a1" + "x".repeat(71))).isFalse();   // over BCrypt's 72 bytes
    }

    @Test
    void locksAfterTooManyFailuresAndResetsOnSuccess() {
        var service = new LoginAttemptService(new AppProperties("", "UTC", null, null,
                new AppProperties.Auth(false, "", "k", 14, 3, 15), null));
        for (int i = 0; i < 2; i++) service.recordFailure("login:a@test");
        assertThat(service.isBlocked("login:a@test")).isFalse();
        service.recordFailure("login:a@test");
        assertThat(service.isBlocked("login:a@test")).isTrue();
        assertThat(service.isBlocked("login:b@test")).isFalse();
        service.reset("login:a@test");
        assertThat(service.isBlocked("login:a@test")).isFalse();
    }

    @Test
    void tokensAreRandomAndOnlyHashesAreComparable() {
        String a = TokenGenerator.newToken();
        assertThat(a).isNotEqualTo(TokenGenerator.newToken()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(TokenGenerator.sha256(a)).hasSize(64).isEqualTo(TokenGenerator.sha256(a));
        assertThat(TokenGenerator.shortCode(6)).matches("[A-HJ-NP-Z2-9]{6}");
    }
}
