package com.ecoswap.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = "Enter your full name.") @Size(min = 2, max = 120, message = "Use 2–120 characters.") String fullName,
        @NotBlank(message = "Enter your university.") @Size(min = 2, max = 150, message = "Use 2–150 characters.") String university) {}
