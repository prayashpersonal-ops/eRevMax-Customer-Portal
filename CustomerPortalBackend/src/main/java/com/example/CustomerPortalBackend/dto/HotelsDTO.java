package com.example.CustomerPortalBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HotelsDTO {
    private UUID id;
    private UUID userHotelId;
    private String name;
    private Boolean active;
    private Boolean hotelActive;
    private Boolean userHotelActive;
    private Boolean sellable;
    private List<UserDTO> users;
    private List<RoomTypeDTO> roomTypes;
    private List<RatePlanDTO> ratePlans;
}
