package com.example.CustomerPortalBackend.repository;

import com.example.CustomerPortalBackend.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoomTypesRepository extends JpaRepository<RoomType, UUID> {
    Optional<RoomType> findByRoomTypeCode(String roomTypeCode);
}
