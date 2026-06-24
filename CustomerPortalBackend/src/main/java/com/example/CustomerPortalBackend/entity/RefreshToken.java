package com.example.CustomerPortalBackend.entity;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "refresh_token", indexes = {
                @Index(name = "refresh_token_jti_idx",columnList = "jti", unique = true),
                @Index(name = "refresh_token_user_id_idx", columnList = "user_id"),
                @Index(name = "refresh_token_admin_id_idx", columnList = "admin_id"),
                @Index(name = "refresh_token_expires_at_idx", columnList = "expiresAt")
})
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "jti", nullable = false, unique = true)
    private String jti;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", unique = true)
    private Admin admin;

    @Column(nullable = false)
    private Boolean revoked;

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Column(nullable = false)
    private Date createdAt;

    @Column(nullable = false)
    private Date expiresAt;

    @Column(columnDefinition = "TEXT")
    private String replacedByToken;

    @PrePersist
    public void validate() {
        if ((user == null && admin == null) || (user != null && admin != null)) {
            throw new IllegalStateException("RefreshToken must belong to exactly one principal");
        }
        if (revoked == null) {
            revoked = false;
        }
        if (createdAt == null) {
            createdAt = Date.from(Instant.now());
        }
    }
}
