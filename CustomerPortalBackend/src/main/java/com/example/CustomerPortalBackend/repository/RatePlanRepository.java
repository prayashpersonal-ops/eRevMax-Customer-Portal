package com.example.CustomerPortalBackend.repository;

import com.example.CustomerPortalBackend.entity.Hotel;
import com.example.CustomerPortalBackend.entity.RatePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RatePlanRepository extends JpaRepository<RatePlan, UUID> {

    Optional<RatePlan> findByHotelAndRatePlanCode(Hotel hotel, String s);
}
