package com.ecoswap.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketForm(
        @NotBlank(message = "Add a short subject.") @Size(min = 4, max = 150, message = "Use 4–150 characters.") String subject,
        @NotBlank(message = "Describe the issue.") @Size(min = 10, max = 4000, message = "Use 10–4000 characters.") String message) {}
