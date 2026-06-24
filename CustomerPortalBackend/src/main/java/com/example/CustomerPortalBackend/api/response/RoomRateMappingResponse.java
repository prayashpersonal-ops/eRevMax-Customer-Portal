package com.example.CustomerPortalBackend.api.response;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record RoomRateMappingResponse(
        UUID roomRateMappingId,
        String hotelName,

        String roomTypeCode,
        String roomTypeName,

        String ratePlanCode,
        String ratePlanName,

        BigDecimal baseRate,
        BigDecimal taxesAndFee,
        BigDecimal feeCollectedByHotel,
        BigDecimal totalTripCost,
        BigDecimal agentEarningsPercent,
        BigDecimal hotelReceives,

        Integer occupancy,

        BigDecimal cancellationCharge,

        LocalDateTime payLaterDeadline,

        Boolean breakfastIncluded,

        Boolean active
) {
}
