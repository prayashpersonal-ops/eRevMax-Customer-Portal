package com.example.CustomerPortalBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTypeDTO {
    private UUID id;

    // Room Information
    private String roomTypeCode;
    private String roomTypeName;
    private String shortDescription;
    private String longDescription;
    private String roomCategory;

    // Occupancy Setup
    private Integer maxRoomCapacity;
    private Integer maxAdults;
    private Integer maxChildren;
    private Integer baseOccupancy;

    // Room Details
    private String roomView;
    private Integer roomSize;
    private String units;
    private String bedType;
    private Boolean smokingAllowed;

    // Mapped rate plans for this room type. This lets frontend show one room card with rate plans inside it.
    private List<RatePlanDTO> ratePlans;
}
