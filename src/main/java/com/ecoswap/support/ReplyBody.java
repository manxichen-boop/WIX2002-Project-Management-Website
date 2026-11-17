package com.ecoswap.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplyBody(@NotBlank(message = "Write a message.") @Size(max = 4000, message = "Use 4000 characters or fewer.") String message) {}
