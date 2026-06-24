package com.example.CustomerPortalBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminHotelDTO {
    private UUID userHotelId;
    private UUID hotelId;
    private String hotelName;
    private Boolean hotelActive;
    private Boolean userHotelActive;
    private Boolean sellable;
    private List<RoomTypeDTO> roomTypes;
    private List<RatePlanDTO> ratePlans;
}
