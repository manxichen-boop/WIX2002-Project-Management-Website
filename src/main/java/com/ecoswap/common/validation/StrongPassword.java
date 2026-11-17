package com.ecoswap.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 8–72 characters with at least one letter and one digit (72 = BCrypt's input limit). */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {
    String message() default "Use 8–72 characters with at least one letter and one number.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
