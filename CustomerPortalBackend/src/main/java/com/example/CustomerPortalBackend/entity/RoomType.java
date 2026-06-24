package com.example.CustomerPortalBackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "room_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    //Room Information
    @Column(unique = true,nullable = false)
    private String roomTypeCode;
    private String roomTypeName;
    @Column(length = 150)
    private String shortDescription;
    @Column(length = 1500)
    private String longDescription;
    private String roomCategory;

    //Occupancy SetUp
    private Integer maxRoomCapacity;
    private Integer maxAdults;
    private Integer maxChildren;
    private Integer baseOccupancy;

    private String roomView;
    private Integer roomSize;
    private String units;
    private String bedType;
    private Boolean smokingAllowed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    @JsonIgnore
    private Hotel hotel;

    @OneToMany(mappedBy = "roomType",cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonIgnore
    private List<RoomRateMapping> roomRateMappings;
}