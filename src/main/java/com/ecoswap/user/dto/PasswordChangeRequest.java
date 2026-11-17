package com.ecoswap.user.dto;

import com.ecoswap.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank(message = "Enter your current password.") @Size(max = 200) String currentPassword,
        @NotBlank(message = "Choose a new password.") @StrongPassword String newPassword,
        @NotBlank(message = "Confirm your new password.") String confirmPassword) {}
