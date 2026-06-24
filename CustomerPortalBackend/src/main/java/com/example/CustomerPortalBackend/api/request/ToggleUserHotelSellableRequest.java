package com.example.CustomerPortalBackend.api.request;

import java.util.UUID;

public record ToggleUserHotelSellableRequest(
        String email,
        UUID hotelId,
        String hotelName,
        UUID userHotelId,
        Boolean sellable
) {
}
