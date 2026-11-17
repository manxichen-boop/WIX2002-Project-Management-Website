package com.ecoswap.auth.dto;

public record RegisterResponse(String message, String email, boolean verificationEmailSent) {}
