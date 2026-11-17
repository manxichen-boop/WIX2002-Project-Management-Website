package com.ecoswap.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryForm(
        @NotBlank(message = "Enter a name.") @Size(min = 2, max = 80, message = "Use 2–80 characters.") String name,
        @Size(max = 255, message = "Use 255 characters or fewer.") String description,
        @Pattern(regexp = "books|electronics|clothing|accessories|furniture|stationery|sports|household|reusable|more", message = "Choose one of the available icons.") String iconKey,
        @Min(value = 0, message = "Use 0 or more.") @Max(value = 9999, message = "Use 9999 or less.") Integer displayOrder) {}
