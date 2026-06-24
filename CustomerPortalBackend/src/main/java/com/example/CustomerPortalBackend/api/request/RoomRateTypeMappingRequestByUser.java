package com.example.CustomerPortalBackend.api.request;

public record RoomRateTypeMappingRequestByUser(
        String hotelName,
        String roomTypeCode,
        String ratePlanCode
) {
}