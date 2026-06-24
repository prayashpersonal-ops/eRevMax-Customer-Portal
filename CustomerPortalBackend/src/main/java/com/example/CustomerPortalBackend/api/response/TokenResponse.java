package com.example.CustomerPortalBackend.api.response;

import lombok.Builder;

@Builder
public record TokenResponse(
        Boolean success,
        String message,
        String accessToken,
        String refreshToken,
        long expiresIn,
        String TokenType
){
    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn){
        return new TokenResponse(true, "" ,accessToken, refreshToken, expiresIn, "Bearer");
    }
}