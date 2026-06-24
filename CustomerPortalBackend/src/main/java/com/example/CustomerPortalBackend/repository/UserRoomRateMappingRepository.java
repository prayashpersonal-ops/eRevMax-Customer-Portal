package com.example.CustomerPortalBackend.repository;

import com.example.CustomerPortalBackend.entity.RoomRateMapping;
import com.example.CustomerPortalBackend.entity.User;
import com.example.CustomerPortalBackend.entity.UserRoomRateMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRoomRateMappingRepository extends JpaRepository<UserRoomRateMapping, UUID> {
    boolean existsByUserAndRoomRateMapping(User user, RoomRateMapping mapping);
}
