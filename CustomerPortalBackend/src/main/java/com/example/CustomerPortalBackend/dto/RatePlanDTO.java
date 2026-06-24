package com.example.CustomerPortalBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatePlanDTO {
    private UUID id;

    // Rate Plan Details
    private String ratePlanCode;
    private String ratePlanName;

    // Booking Window
    private LocalDate bookingStartDate;
    private LocalDate bookingEndDate;

    // Selling Window
    private LocalDate sellingStartDate;
    private LocalDate sellingEndDate;

    // Description
    private String shortDescription;
    private String longDescription;

    // Classification
    private String rateType;
    private String rateCategory;

    // Mapping Codes
    private String marketCode;
    private String sourceCode;
}
