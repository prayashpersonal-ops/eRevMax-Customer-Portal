package com.example.CustomerPortalBackend.repository;

import com.example.CustomerPortalBackend.entity.User;
import com.example.CustomerPortalBackend.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(
            String name, String email, String companyName, Pageable pageable
    );

    List<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(
            String name, String email, String companyName, Sort sort
    );


    @Query("""
            SELECT u FROM User u
            WHERE (u.status IS NULL OR u.status <> :hiddenStatus)
            AND (
                :search IS NULL OR :search = ''
                OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.companyName) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            """)
    Page<User> searchVisibleUsers(
            @Param("hiddenStatus") UserStatus hiddenStatus,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.status IS NULL OR u.status <> :hiddenStatus
            """)
    long countVisibleUsers(@Param("hiddenStatus") UserStatus hiddenStatus);


    long countByStatus(UserStatus status);
}
