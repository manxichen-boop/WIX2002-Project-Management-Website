package com.ecoswap.auth.dto;

/** Where the browser should go after a successful login, plus the signed-in user. */
public record LoginResponse(String redirectUrl, MeResponse user) {}
