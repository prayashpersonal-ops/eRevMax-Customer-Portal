package com.example.CustomerPortalBackend.repository;

import com.example.CustomerPortalBackend.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelRepository extends JpaRepository<Hotel, UUID> {

    Optional<List<Hotel>> findByNameContainingIgnoreCase(String name);

    Optional<Hotel> findByNameIgnoreCase(String name);
}
