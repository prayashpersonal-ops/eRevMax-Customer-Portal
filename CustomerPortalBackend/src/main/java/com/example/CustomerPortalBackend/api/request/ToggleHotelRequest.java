package com.example.CustomerPortalBackend.api.request;

import java.util.UUID;

public record ToggleHotelRequest(
        UUID userHotelId,
        boolean active
) {
}
