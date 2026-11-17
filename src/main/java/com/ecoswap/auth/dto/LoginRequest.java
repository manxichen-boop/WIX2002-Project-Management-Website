package com.ecoswap.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Enter your e-mail.") @Size(max = 190) String email,
        @NotBlank(message = "Enter your password.") @Size(max = 200) String password,
        boolean rememberMe) {}
