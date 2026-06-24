package com.example.CustomerPortalBackend.api.request;

import lombok.Builder;

@Builder
public record SearchHotelsRequest (
    String name
) {}