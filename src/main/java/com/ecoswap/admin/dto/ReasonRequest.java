package com.ecoswap.admin.dto;

import jakarta.validation.constraints.Size;

/** Optional/required free-text reason for a moderation decision (required-ness is checked per action). */
public record ReasonRequest(@Size(max = 500, message = "Use 500 characters or fewer.") String reason) {}
