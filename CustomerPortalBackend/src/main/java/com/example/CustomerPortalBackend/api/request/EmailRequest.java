package com.example.CustomerPortalBackend.api.request;

import jakarta.validation.constraints.Email;

public record EmailRequest(
        @Email(message = "Enter valid Email") String email
) {}
