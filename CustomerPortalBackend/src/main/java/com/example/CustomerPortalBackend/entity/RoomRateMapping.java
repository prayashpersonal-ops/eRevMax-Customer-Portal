package com.example.CustomerPortalBackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "room_rate_mapping",
        uniqueConstraints = {@UniqueConstraint(
                name = "uk_room_rate",
                columnNames = {"room_type_id", "rate_plan_id"}
        )}
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRateMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_plan_id")
    private RatePlan ratePlan;

    @Column(name = "base_rate", precision = 10, scale = 2)
    private BigDecimal baseRate;

    @Column(name = "taxes_and_fee", precision = 10, scale = 2)
    private BigDecimal taxesAndFee;

    @Column(name = "fee_collected_by_hotel", precision = 10, scale = 2)
    private BigDecimal feeCollectedByHotel;

    @Column(name = "total_trip_cost", precision = 10, scale = 2)
    private BigDecimal totalTripCost;

    @Column(name = "agent_earnings_percent", precision = 5, scale = 2)
    private BigDecimal agentEarningsPercent;

    @Column(name = "hotel_receives", precision = 10, scale = 2)
    private BigDecimal hotelReceives;

    @Column(name = "occupancy")
    private Integer occupancy;

    @Column(name = "cancellation_charge", precision = 10, scale = 2)
    private BigDecimal cancellationCharge;

    @Column(name = "pay_later_deadline")
    private LocalDateTime payLaterDeadline;

    @Column(name = "breakfast_included")
    private Boolean breakfastIncluded;

    @Column(name = "active")
    private Boolean active;

    @OneToMany(mappedBy = "roomRateMapping")
    private List<UserRoomRateMapping> userMappings;

    @PrePersist
    public void onCreate() {
        if (active == null) active = true;
        if (occupancy == null && roomType != null) occupancy = roomType.getBaseOccupancy();
    }
}