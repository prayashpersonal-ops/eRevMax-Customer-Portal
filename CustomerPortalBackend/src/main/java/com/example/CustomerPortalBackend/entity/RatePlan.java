package com.example.CustomerPortalBackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rate_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Rate Plan Details
    @Column(unique = true,nullable = false)
    private String ratePlanCode;

    @Column(nullable = false)
    private String ratePlanName;

    // Booking Window
    private LocalDate bookingStartDate;

    private LocalDate bookingEndDate;

    // Selling Window
    private LocalDate sellingStartDate;

    private LocalDate sellingEndDate;

    // Description
    @Column(length = 150)
    private String shortDescription;

    @Column(length = 1500)
    private String longDescription;

    // Classification
    private String rateType;

    private String rateCategory;

    // Mapping Codes
    private String marketCode;

    private String sourceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    @JsonIgnore
    private Hotel hotel;

    @OneToMany(mappedBy = "ratePlan",cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonIgnore
    private List<RoomRateMapping> roomRateMappings;
}