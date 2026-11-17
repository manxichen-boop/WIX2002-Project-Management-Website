package com.ecoswap.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "Enter your e-mail.") @Email(message = "Enter a valid e-mail address.") @Size(max = 190) String email) {}
