package com.example.CustomerPortalBackend.entity;

import com.example.CustomerPortalBackend.enums.Provider;
import com.example.CustomerPortalBackend.enums.Role;
import com.example.CustomerPortalBackend.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Email(message = "Enter a valid Email")
    @NotBlank(message = "Email is required")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    private String name;

    private String password;

    private String companyName;

    private String address;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<UserHotel> userHotels;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean enable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    private String providerId;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRoomRateMapping> roomMappings;

    @PrePersist
    public void prePersist() {
        if (enable == null) enable = false;
        if (status == null) status = Boolean.TRUE.equals(enable) ? UserStatus.APPROVED : UserStatus.PENDING;
        if (role == null) role = Role.USER;
        if (provider == null) provider = Provider.LOCAL;
    }

    @PreUpdate
    public void preUpdate() {
        if (status == null) status = Boolean.TRUE.equals(enable) ? UserStatus.APPROVED : UserStatus.PENDING;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enable != null && this.enable;
    }
}
