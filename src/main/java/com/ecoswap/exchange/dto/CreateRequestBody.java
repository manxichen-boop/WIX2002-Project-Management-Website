package com.ecoswap.exchange.dto;

import jakarta.validation.constraints.Size;

/** offeredItemId is required for SWAP listings and must be null for GIVEAWAY / UPCYCLING. */
public record CreateRequestBody(Long offeredItemId, @Size(max = 500, message = "Use 500 characters or fewer.") String message) {}
