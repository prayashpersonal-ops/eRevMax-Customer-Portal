package com.example.CustomerPortalBackend.service.implementations;

import com.example.CustomerPortalBackend.api.request.ToggleHotelRequest;
import com.example.CustomerPortalBackend.api.response.UserPageAbleResponse;
import com.example.CustomerPortalBackend.dto.*;
import com.example.CustomerPortalBackend.entity.*;
import com.example.CustomerPortalBackend.api.request.LoginRequest;
import com.example.CustomerPortalBackend.api.request.RefreshTokenRequest;
import com.example.CustomerPortalBackend.api.response.ApiDataResponse;
import com.example.CustomerPortalBackend.api.response.ApiResponse;
import com.example.CustomerPortalBackend.api.response.TokenResponse;
import com.example.CustomerPortalBackend.enums.UserStatus;
import com.example.CustomerPortalBackend.repository.AdminRepository;
import com.example.CustomerPortalBackend.repository.RefreshTokenRepository;
import com.example.CustomerPortalBackend.repository.UserHotelRepository;
import com.example.CustomerPortalBackend.repository.UserRepository;
import com.example.CustomerPortalBackend.security.CookieService;
import com.example.CustomerPortalBackend.security.JwtService;
import com.example.CustomerPortalBackend.service.AdminService;
import com.example.CustomerPortalBackend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RefreshTokenRepository refreshTokenRepository;
    public final AuthService authService;
    private final UserHotelRepository userHotelRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public TokenResponse logInAdmin(LoginRequest loginRequest, HttpServletResponse response) {
        if (loginRequest == null) {
            throw new RuntimeException("Admin is null");
        }
        Optional<Admin> existingAdmin = adminRepository.findByEmail(loginRequest.email());
        if (existingAdmin.isEmpty()) {
            throw new RuntimeException("Admin not found");
        }
        Admin admin = existingAdmin.get();
        if (admin.getPassword() == null || !loginRequest.password().equals(admin.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        String newJti = UUID.randomUUID().toString();
        String newAccessToken = jwtService.generateAccessToken(admin);
        String newRefreshToken = jwtService.generateRefreshedToken(admin, newJti);
        logger.info("Admin {} logged in", admin.getEmail());
        RefreshToken adminRefreshToken = refreshTokenRepository.getRefreshTokenByAdmin(admin);
        if (adminRefreshToken == null) {
            adminRefreshToken = new RefreshToken();
        }
        adminRefreshToken.setJti(newJti);
        adminRefreshToken.setAdmin(admin);
        adminRefreshToken.setAccessToken(newAccessToken);
        adminRefreshToken.setRevoked(false);
        adminRefreshToken.setCreatedAt(Date.from(Instant.now()));
        adminRefreshToken.setExpiresAt(Date.from(Instant.now().plusSeconds(jwtService.getRefreshTokenValiditySeconds())));
        refreshTokenRepository.save(adminRefreshToken);
        cookieService.attachRefreshCookie(response, newRefreshToken, (int) jwtService.getRefreshTokenValiditySeconds());
        cookieService.addNoStoreHeaders(response);
        logger.info("For Admin:{} Cookie is created", admin.getEmail());
        return TokenResponse.builder()
                .success(true)
                .message("Admin logged in successfully")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .TokenType("Bearer ")
                .expiresIn(jwtService.getAccessTokenValiditySeconds())
                .build();
    }

    @Override
    public ApiDataResponse<UserPageAbleResponse> seeAllUsers(
            String token,
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortOrder,
            String search
    ) {
        Optional<Admin> optionalAdmin = authService.authenticateAdmin(token);
        if (optionalAdmin.isEmpty()) {
            logger.error("Invalid token");
            return ApiDataResponse.<UserPageAbleResponse>builder()
                    .success(false)
                    .message("Invalid token")
                    .data(null)
                    .timestamp(Instant.now())
                    .build();
        }

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        String keyword = search == null ? "" : search.trim();
        Page<User> usersPage = userRepository.searchVisibleUsers(UserStatus.DENIED, keyword, pageable);

        List<AdminUserDTO> userDTOS = usersPage.getContent().stream().map(this::mapToAdminUserDTO).toList();

        UserPageAbleResponse userPageAbleResponse = new UserPageAbleResponse();
        userPageAbleResponse.setContent(userDTOS);
        userPageAbleResponse.setPageNumber(usersPage.getNumber());
        userPageAbleResponse.setPageSize(usersPage.getSize());
        userPageAbleResponse.setTotalElements(usersPage.getTotalElements());
        userPageAbleResponse.setTotalPages(usersPage.getTotalPages());
        userPageAbleResponse.setLastPage(usersPage.isLast());
        userPageAbleResponse.setSortBy(sortBy);
        userPageAbleResponse.setSortOrder(sortOrder);
        userPageAbleResponse.setStats(getUserStats());

        logger.info("Found {} users in database", userDTOS.size());
        return ApiDataResponse.<UserPageAbleResponse>builder()
                .success(true)
                .message("All users fetched successfully")
                .data(userPageAbleResponse)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public ApiResponse userAccessDeniedByEmail(String email, String token) {
        Optional<Admin> optionalAdmin = authService.authenticateAdmin(token);
        if (optionalAdmin.isEmpty()) {
            logger.error("Invalid token");
            return buildErrorApiResponse();
        }

        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            User userEntity = user.get();
            boolean wasApprovedOrLoggedIn = Boolean.TRUE.equals(userEntity.getEnable()) || userEntity.getStatus() == UserStatus.APPROVED;

            userEntity.setEnable(false);
            userEntity.setStatus(wasApprovedOrLoggedIn ? UserStatus.BLOCKED : UserStatus.DENIED);

            if (!wasApprovedOrLoggedIn) {
                userHotelRepository.deleteAll(userHotelRepository.findByUser(userEntity));
            }

            userRepository.save(userEntity);
            revokeRefreshTokenForUser(userEntity);

            return ApiResponse.builder()
                    .success(true)
                    .message(wasApprovedOrLoggedIn
                            ? "User blocked successfully"
                            : "User denied successfully. They must sign up again to send a new request")
                    .timestamp(Instant.now())
                    .build();
        }
        return ApiResponse.builder()
                .success(false)
                .message("User not found")
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public ApiResponse userAccessGrantedByEmail(String email, String token) {
        Optional<Admin> optionalAdmin = authService.authenticateAdmin(token);
        if (optionalAdmin.isEmpty()) {
            return buildErrorApiResponse();
        }
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            User userEntity = user.get();
            userEntity.setEnable(true);
            userEntity.setStatus(UserStatus.APPROVED);
            userRepository.save(userEntity);
            return ApiResponse.builder()
                    .success(true)
                    .message("User access granted successfully")
                    .timestamp(Instant.now())
                    .build();
        }
        return ApiResponse.builder()
                .success(false)
                .message("User not found")
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest, HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = authService.readRefreshTokenFromRequest(refreshTokenRequest, request)
                .orElseThrow(() -> new BadCredentialsException("Refresh Token is Missing"));
        RefreshToken storedRefreshToken = authService.refreshTokenValidity(refreshToken);
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid Refresh Token");
        }
        String adminEmail = jwtService.getUserDetailsEmail(refreshToken);
        if (!jwtService.getRoles(refreshToken).contains("ROLE_ADMIN")) {
            throw new BadCredentialsException("Refresh Token does not belong to an admin");
        }
        if (!storedRefreshToken.getAdmin().getEmail().equals(adminEmail)) {
            throw new BadCredentialsException("Refresh Token does not belong to the admin");
        }
        Admin admin = storedRefreshToken.getAdmin();
        RefreshToken adminRefreshToken = refreshTokenRepository.getRefreshTokenByAdmin(admin);
        String newJti = UUID.randomUUID().toString();
        String newAccessToken = jwtService.generateAccessToken(admin);
        String newRefreshToken = jwtService.generateRefreshedToken(admin, newJti);
        adminRefreshToken.setJti(newJti);
        adminRefreshToken.setAccessToken(newAccessToken);
        adminRefreshToken.setRevoked(false);
        adminRefreshToken.setCreatedAt(Date.from(Instant.now()));
        adminRefreshToken.setExpiresAt(Date.from(Instant.now().plusSeconds(jwtService.getRefreshTokenValiditySeconds())));
        adminRefreshToken.setReplacedByToken(refreshToken);
        refreshTokenRepository.save(adminRefreshToken);
        cookieService.attachRefreshCookie(response, newRefreshToken, (int) jwtService.getRefreshTokenValiditySeconds());
        cookieService.addNoStoreHeaders(response);
        return TokenResponse.builder()
                .success(true)
                .message("Refresh Token successfully refreshed")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
    }

    @Override
    public ApiResponse updateHotelStatus(ToggleHotelRequest request, String token) {
        Optional<Admin> optionalAdmin = authService.authenticateAdmin(token);
        if (optionalAdmin.isEmpty()) {
            return buildErrorApiResponse();
        }
        Optional<UserHotel> optionalUserHotel = userHotelRepository.findById(request.userHotelId());
        if (optionalUserHotel.isEmpty()) {
            return ApiResponse.builder().success(false)
                    .message("User hotel not found").timestamp(Instant.now()).build();
        }
        UserHotel userHotel = optionalUserHotel.get();
        userHotel.setActive(request.active());
        userHotelRepository.save(userHotel);
        return ApiResponse.builder()
                .success(true)
                .message("Hotel status updated successfully")
                .timestamp(Instant.now())
                .build();
    }

    private UserStatsDTO getUserStats() {
        return UserStatsDTO.builder()
                .totalUsers(userRepository.countVisibleUsers(UserStatus.DENIED))
                .pendingUsers(userRepository.countByStatus(UserStatus.PENDING))
                .approvedUsers(userRepository.countByStatus(UserStatus.APPROVED))
                .deniedUsers(0)
                .blockedUsers(userRepository.countByStatus(UserStatus.BLOCKED))
                .build();
    }

    private void revokeRefreshTokenForUser(User user) {
        RefreshToken refreshToken = refreshTokenRepository.getRefreshTokenByUser(user);
        if (refreshToken != null) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        }
    }

    private AdminUserDTO mapToAdminUserDTO(User user) {
        List<AdminHotelDTO> hotels = user.getUserHotels()
                .stream()
                .map(userHotel -> {
                    Hotel hotel = userHotel.getHotel();
                    return AdminHotelDTO.builder()
                            .userHotelId(userHotel.getId())
                            .hotelId(hotel.getId())
                            .hotelName(hotel.getName())
                            .hotelActive(hotel.getActive())
                            .userHotelActive(userHotel.getActive())
                            .sellable(userHotel.getSellable())
                            .roomTypes(hotel.getRoomTypes().stream()
                                    .map(roomType -> RoomTypeDTO.builder()
                                            .roomTypeCode(roomType.getRoomTypeCode())
                                            .roomTypeName(roomType.getRoomTypeName())
                                            .roomCategory(roomType.getRoomCategory())
                                            .roomView(roomType.getRoomView())
                                            .roomSize(roomType.getRoomSize())
                                            .bedType(roomType.getBedType())
                                            .maxAdults(roomType.getMaxAdults())
                                            .maxChildren(roomType.getMaxChildren())
                                            .smokingAllowed(roomType.getSmokingAllowed())
                                            .longDescription(roomType.getLongDescription())
                                            .shortDescription(roomType.getShortDescription())
                                            .units(roomType.getUnits())
                                            .maxRoomCapacity(roomType.getMaxRoomCapacity())
                                            .baseOccupancy(roomType.getBaseOccupancy())
                                            .build()).toList())
                            .ratePlans(hotel.getRatePlans().stream()
                                    .map(ratePlan -> RatePlanDTO.builder()
                                            .ratePlanCode(ratePlan.getRatePlanCode())
                                            .ratePlanName(ratePlan.getRatePlanName())
                                            .rateType(ratePlan.getRateType())
                                            .rateCategory(ratePlan.getRateCategory())
                                            .marketCode(ratePlan.getMarketCode())
                                            .sourceCode(ratePlan.getSourceCode())
                                            .bookingEndDate(ratePlan.getBookingEndDate())
                                            .bookingStartDate(ratePlan.getBookingStartDate())
                                            .sellingEndDate(ratePlan.getSellingEndDate())
                                            .sellingStartDate(ratePlan.getSellingStartDate())
                                            .shortDescription(ratePlan.getShortDescription())
                                            .longDescription(ratePlan.getLongDescription())
                                            .build()).toList())
                            .build();
                }).toList();

        UserStatus status = user.getStatus();
        if (status == null) {
            status = Boolean.TRUE.equals(user.getEnable()) ? UserStatus.APPROVED : UserStatus.PENDING;
        }

        return AdminUserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .provider(user.getProvider())
                .enable(user.getEnable())
                .status(status)
                .hotels(hotels)
                .companyName(user.getCompanyName())
                .address(user.getAddress())
                .build();
    }

    private ApiResponse buildErrorApiResponse() {
        return ApiResponse.builder()
                .success(false)
                .message("Invalid token")
                .timestamp(Instant.now())
                .build();
    }
}
