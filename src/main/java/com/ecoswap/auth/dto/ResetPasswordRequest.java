package com.ecoswap.auth.dto;

import com.ecoswap.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "This reset link is incomplete.") @Size(max = 100) String token,
        @NotBlank(message = "Choose a new password.") @StrongPassword String password,
        @NotBlank(message = "Confirm your new password.") String confirmPassword) {}
