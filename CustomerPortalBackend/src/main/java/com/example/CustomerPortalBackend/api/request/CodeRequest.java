package com.example.CustomerPortalBackend.api.request;

import lombok.Builder;

@Builder
public record CodeRequest (
        String code
){
}