package com.ecoswap.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return isStrong(value);
    }

    public static boolean isStrong(String value) {
        if (value == null || value.length() < 8 || value.getBytes(StandardCharsets.UTF_8).length > 72) {
            return false;
        }
        return value.chars().anyMatch(Character::isLetter) && value.chars().anyMatch(Character::isDigit);
    }
}
