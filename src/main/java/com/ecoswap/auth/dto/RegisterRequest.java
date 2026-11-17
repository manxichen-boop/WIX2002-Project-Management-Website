package com.ecoswap.auth.dto;

import com.ecoswap.common.validation.StrongPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Enter your full name.") @Size(min = 2, max = 120, message = "Use 2–120 characters.") String fullName,
        @NotBlank(message = "Enter your university e-mail.") @Email(message = "Enter a valid e-mail address.")
        @Size(max = 190, message = "That e-mail is too long.") String email,
        @NotBlank(message = "Choose a password.") @StrongPassword String password,
        @NotBlank(message = "Confirm your password.") String confirmPassword,
        @NotBlank(message = "Enter your university.") @Size(min = 2, max = 150, message = "Use 2–150 characters.") String university,
        @AssertTrue(message = "Please accept the community guidelines to continue.") boolean acceptTerms) {}
