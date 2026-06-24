package com.example.CustomerPortalBackend.repository;

import com.example.CustomerPortalBackend.entity.Admin;
import com.example.CustomerPortalBackend.entity.RefreshToken;
import com.example.CustomerPortalBackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByJti(String jti);

    RefreshToken getRefreshTokenByUser(User user);

    RefreshToken getRefreshTokenByAdmin(Admin admin);
}
