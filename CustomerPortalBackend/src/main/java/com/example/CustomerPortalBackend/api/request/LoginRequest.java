package com.example.CustomerPortalBackend.api.request;


public record LoginRequest(
        String email,
        String password
) {}