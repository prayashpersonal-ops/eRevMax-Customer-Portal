package com.example.CustomerPortalBackend.repository;

import com.example.CustomerPortalBackend.entity.RatePlan;
import com.example.CustomerPortalBackend.entity.RoomRateMapping;
import com.example.CustomerPortalBackend.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRateMappingRepository extends JpaRepository<RoomRateMapping, UUID> {

    List<RoomRateMapping> findByRoomTypeAndRatePlan(RoomType roomType, RatePlan ratePlan);

    List<RoomRateMapping> findByRoomType(RoomType roomType);
}
