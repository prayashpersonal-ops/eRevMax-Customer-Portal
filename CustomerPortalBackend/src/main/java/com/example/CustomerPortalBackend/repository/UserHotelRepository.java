package com.example.CustomerPortalBackend.repository;
import com.example.CustomerPortalBackend.entity.Hotel;
import com.example.CustomerPortalBackend.entity.User;
import com.example.CustomerPortalBackend.entity.UserHotel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserHotelRepository extends JpaRepository<UserHotel, UUID> {

    boolean existsByUserIdAndHotelId(UUID id, UUID id1);

    boolean existsByUserAndHotel(User user, Hotel hotel);

    Optional<UserHotel> findByUserAndHotel(User user, Hotel hotel);

    List<UserHotel> findByUser(User user);
}
