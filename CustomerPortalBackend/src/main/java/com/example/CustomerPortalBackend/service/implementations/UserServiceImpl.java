package com.example.CustomerPortalBackend.service.implementations;

import com.example.CustomerPortalBackend.api.request.*;
import com.example.CustomerPortalBackend.api.response.ApiDataResponse;
import com.example.CustomerPortalBackend.dto.HotelsDTO;
import com.example.CustomerPortalBackend.dto.UserDTO;
import com.example.CustomerPortalBackend.entity.*;
import com.example.CustomerPortalBackend.enums.Provider;
import com.example.CustomerPortalBackend.enums.UserStatus;
import com.example.CustomerPortalBackend.api.response.ApiResponse;
import com.example.CustomerPortalBackend.api.response.TokenResponse;
import com.example.CustomerPortalBackend.repository.HotelRepository;
import com.example.CustomerPortalBackend.repository.RefreshTokenRepository;
import com.example.CustomerPortalBackend.repository.UserHotelRepository;
import com.example.CustomerPortalBackend.repository.UserRepository;
import com.example.CustomerPortalBackend.security.CookieService;
import com.example.CustomerPortalBackend.security.JwtService;
import com.example.CustomerPortalBackend.service.AuthService;
import com.example.CustomerPortalBackend.service.UserService;
import com.nimbusds.oauth2.sdk.util.singleuse.AlreadyUsedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final HotelRepository hotelRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserHotelRepository userHotelRepository;
    private final CookieService cookieService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthService authService;

    @Override
    public ApiResponse createUser(UserDTO userDTO) {
        Optional<User> existingUser = userRepository.findByEmail(userDTO.getEmail());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            UserStatus status = user.getStatus();
            if (status == null) {
                status = Boolean.TRUE.equals(user.getEnable()) ? UserStatus.APPROVED : UserStatus.PENDING;
            }

            if (status == UserStatus.DENIED) {
                user.setName(userDTO.getName());
                user.setCompanyName(userDTO.getCompanyName());
                user.setAddress(userDTO.getAddress());
                user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
                user.setEnable(false);
                user.setStatus(UserStatus.PENDING);
                user.setProvider(Provider.LOCAL);
                userRepository.save(user);

                return ApiResponse.builder()
                        .success(true)
                        .message("Request sent again successfully. Please wait for admin approval")
                        .timestamp(Instant.now())
                        .build();
            }

            if (status == UserStatus.PENDING) {
                return ApiResponse.builder()
                        .success(false)
                        .message("Your request is already pending for admin approval")
                        .timestamp(Instant.now())
                        .build();
            }

            if (status == UserStatus.BLOCKED) {
                return ApiResponse.builder()
                        .success(false)
                        .message("This account is blocked. Please contact admin")
                        .timestamp(Instant.now())
                        .build();
            }

            return ApiResponse.builder()
                    .success(false)
                    .message("User already exists")
                    .timestamp(Instant.now())
                    .build();
        }

        User user = modelMapper.map(userDTO, User.class);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setEnable(false);
        user.setStatus(UserStatus.PENDING);
        user.setProvider(Provider.LOCAL);
        userRepository.save(user);

        return ApiResponse.builder()
                .success(true)
                .message("User created successfully")
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public TokenResponse loginUser(LoginRequest loginRequest, HttpServletResponse response) {
        if (loginRequest.email() == null || loginRequest.password() == null) {
            return buildErrorResponse("Email and password are required");
        }
        Optional<User> optionalUser = userRepository.findByEmail(loginRequest.email());
        if (optionalUser.isEmpty()) {
            return buildErrorResponse("Invalid email or password");
        }
        User user = optionalUser.get();
        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword()) && user.getProvider() == Provider.LOCAL) {
            return buildErrorResponse("Invalid password");
        }
        if (!Boolean.TRUE.equals(user.getEnable())) {
            return buildErrorResponse("User account is disabled");
        }
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        userDTO.setPassword(null);
        String newJti = UUID.randomUUID().toString();
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshedToken(user, newJti);
        RefreshToken userRefreshToken = refreshTokenRepository.getRefreshTokenByUser(user);
        if (userRefreshToken == null) {
            userRefreshToken = new RefreshToken();
        }
        userRefreshToken.setJti(newJti);
        userRefreshToken.setUser(user);
        userRefreshToken.setAccessToken(newAccessToken);
        userRefreshToken.setRevoked(false);
        userRefreshToken.setCreatedAt(Date.from(Instant.now()));
        userRefreshToken.setExpiresAt
                (Date.from(Instant.now().plusSeconds(jwtService.getRefreshTokenValiditySeconds())));
        refreshTokenRepository.save(userRefreshToken);
        cookieService.attachRefreshCookie(response, newRefreshToken,(int)jwtService.getRefreshTokenValiditySeconds());
        cookieService.addNoStoreHeaders(response);
        return TokenResponse.builder()
                .success(true)
                .message("Login successful")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtService.getAccessTokenValiditySeconds())
                .TokenType("Bearer")
                .build();
    }

    @Override
    public ApiDataResponse<List<HotelsDTO>> getListOfHotelsTheUserHave(String token) {
        Optional<User> optionalUser = authService.authenticateUser(token);
        if (optionalUser.isEmpty()) {
            return buildErrorApiResponse("Invalid token");
        }
        User user = optionalUser.get();
        List<HotelsDTO> hotelsDTOList = user.getUserHotels().stream()
                .filter(userHotel -> Boolean.TRUE.equals(userHotel.getActive()))
                .map(this::mapUserHotelToDTO)
                .toList();
        if (hotelsDTOList.isEmpty()) {
            return buildErrorApiResponse("No hotels found for the user");
        }
        return ApiDataResponse.<List<HotelsDTO>>builder()
                .data(hotelsDTOList)
                .success(true)
                .message("Hotels fetched successfully")
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public ApiDataResponse<List<HotelsDTO>> searchListOfHotelsByUser(String token,SearchHotelsRequest searchHotelsRequest) {
        Optional<User> optionalUser = authService.authenticateUser(token);
        if (optionalUser.isEmpty()) {
            return buildErrorApiResponse("Invalid token");
        }
        Optional<List<Hotel>> optionalHotels =
                hotelRepository.findByNameContainingIgnoreCase(searchHotelsRequest.name());
        if (optionalHotels.isEmpty() || optionalHotels.get().isEmpty()) {
            return buildErrorApiResponse("No hotels found");
        }
        List<HotelsDTO> hotelsDTOList = mapHotelsToDTO(optionalHotels.get());
        return ApiDataResponse.<List<HotelsDTO>>builder()
                .data(hotelsDTOList)
                .success(true)
                .message("Hotels fetched successfully")
                .timestamp(Instant.now())
                .build();
    }


    @Override
    public ApiDataResponse<List<HotelsDTO>> toggleListOfHotelsByUserSellable(SearchHotelsRequest searchHotelsRequest, String token) throws AlreadyUsedException {
        Optional<User> optionalUser = authService.authenticateUser(token);
        if (optionalUser.isEmpty()) {
            return buildErrorApiResponse("Invalid token");
        }
        User user = optionalUser.get();
        Optional<List<Hotel>> optionalHotels = hotelRepository.findByNameContainingIgnoreCase(searchHotelsRequest.name());
        if (optionalHotels.isEmpty() || optionalHotels.get().isEmpty()) {
            return buildErrorApiResponse("No hotels found with the given name");
        }
        List<Hotel> hotelsList = optionalHotels.get();
        List<HotelsDTO> hotelsDTOList = new ArrayList<>();
        for (Hotel hotel : hotelsList) {
            UserHotel userHotel = userHotelRepository.findByUserAndHotel(user, hotel)
                            .orElseThrow(() -> new BadCredentialsException("User is not assigned to this hotel"));
            if (!Boolean.TRUE.equals(userHotel.getActive())) {
                return buildErrorApiResponse("You Don't have Access of these Hotel name");
            }else {
                userHotel.setSellable(!Boolean.TRUE.equals(userHotel.getSellable()));
                userHotelRepository.save(userHotel);
                hotelsDTOList.add(mapUserHotelToDTO(userHotel));
            }
        }
        return ApiDataResponse.<List<HotelsDTO>>builder()
                .data(hotelsDTOList)
                .success(true)
                .message("Hotel sellable status updated successfully")
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest, HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = authService.readRefreshTokenFromRequest(refreshTokenRequest,request)
                .orElseThrow(()-> new BadCredentialsException("Refresh Token is Missing"));
        RefreshToken storedRefreshToken = authService.refreshTokenValidity(refreshToken);
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid Refresh Token");
        }
        String userEmail = jwtService.getUserDetailsEmail(refreshToken);
        if (!jwtService.getRoles(refreshToken).contains("ROLE_USER")) {
            throw new BadCredentialsException("Refresh Token does not belong to a user");
        }
        if (!storedRefreshToken.getUser().getEmail().equals(userEmail)) {
            throw new BadCredentialsException("Refresh Token does not belong to the user");
        }
        //Refresh Token Rotate
        User user = storedRefreshToken.getUser();
        if (user == null || !Boolean.TRUE.equals(user.getEnable()) || user.getStatus() != UserStatus.APPROVED) {
            storedRefreshToken.setRevoked(true);
            refreshTokenRepository.save(storedRefreshToken);
            throw new BadCredentialsException("User is blocked or not approved");
        }
        RefreshToken userRefreshToken = refreshTokenRepository.getRefreshTokenByUser(user);
        String newJti = UUID.randomUUID().toString();
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshedToken(user, newJti);
        userRefreshToken.setJti(newJti);
        userRefreshToken.setAccessToken(newAccessToken);
        userRefreshToken.setRevoked(false);
        userRefreshToken.setCreatedAt(Date.from(Instant.now()));
        userRefreshToken.setExpiresAt
                (Date.from(Instant.now().plusSeconds(jwtService.getRefreshTokenValiditySeconds())));
        userRefreshToken.setReplacedByToken(refreshToken);
        refreshTokenRepository.save(userRefreshToken);
        cookieService.attachRefreshCookie(response, newRefreshToken,(int)jwtService.getRefreshTokenValiditySeconds());
        cookieService.addNoStoreHeaders(response);
        return TokenResponse.builder()
                .success(true)
                .message("Refresh Token successfully")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
    }

    private HotelsDTO mapUserHotelToDTO(UserHotel userHotel) {
        Hotel hotel = userHotel.getHotel();
        HotelsDTO dto = modelMapper.map(hotel, HotelsDTO.class);
        dto.setId(hotel.getId());
        dto.setName(hotel.getName());
        dto.setUserHotelId(userHotel.getId());
        dto.setActive(hotel.getActive());
        dto.setHotelActive(hotel.getActive());
        dto.setUserHotelActive(userHotel.getActive());
        dto.setSellable(userHotel.getSellable());
        return dto;
    }

    private List<HotelsDTO> mapHotelsToDTO(List<Hotel> hotelsList) {
        if (hotelsList == null) {
            return List.of();
        }
        return hotelsList.stream().distinct()
                .map(hotel -> modelMapper.map(hotel,HotelsDTO.class)).toList();
    }

    private TokenResponse buildErrorResponse(String message) {
        return TokenResponse.builder()
                .success(false)
                .message(message)
                .accessToken(null)
                .refreshToken(null)
                .expiresIn(0)
                .TokenType(null)
                .build();
    }

    private ApiDataResponse<List<HotelsDTO>> buildErrorApiResponse(String message) {
        return ApiDataResponse.<List<HotelsDTO>>builder()
                .data(null)
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}