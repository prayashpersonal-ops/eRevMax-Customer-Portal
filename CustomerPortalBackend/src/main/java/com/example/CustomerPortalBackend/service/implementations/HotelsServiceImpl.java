package com.example.CustomerPortalBackend.service.implementations;

import com.example.CustomerPortalBackend.api.request.AssignHotelsToUser;
import com.example.CustomerPortalBackend.api.request.RoomRateTypeMappingRequestByUser;
import com.example.CustomerPortalBackend.api.request.ToggleUserHotelSellableRequest;
import com.example.CustomerPortalBackend.api.request.UserHotelRequest;
import com.example.CustomerPortalBackend.api.response.RoomRateMappingResponse;
import com.example.CustomerPortalBackend.dto.HotelsDTO;
import com.example.CustomerPortalBackend.dto.RatePlanDTO;
import com.example.CustomerPortalBackend.dto.RoomTypeDTO;
import com.example.CustomerPortalBackend.entity.Admin;
import com.example.CustomerPortalBackend.entity.Hotel;
import com.example.CustomerPortalBackend.entity.RatePlan;
import com.example.CustomerPortalBackend.entity.RoomRateMapping;
import com.example.CustomerPortalBackend.entity.RoomType;
import com.example.CustomerPortalBackend.entity.User;
import com.example.CustomerPortalBackend.entity.UserHotel;
import com.example.CustomerPortalBackend.entity.UserRoomRateMapping;
import com.example.CustomerPortalBackend.api.response.ApiDataResponse;
import com.example.CustomerPortalBackend.api.response.ApiResponse;
import com.example.CustomerPortalBackend.repository.HotelRepository;
import com.example.CustomerPortalBackend.repository.RatePlanRepository;
import com.example.CustomerPortalBackend.repository.RoomRateMappingRepository;
import com.example.CustomerPortalBackend.repository.RoomTypesRepository;
import com.example.CustomerPortalBackend.repository.UserHotelRepository;
import com.example.CustomerPortalBackend.repository.UserRepository;
import com.example.CustomerPortalBackend.repository.UserRoomRateMappingRepository;
import com.example.CustomerPortalBackend.security.JwtService;
import com.example.CustomerPortalBackend.service.AuthService;
import com.example.CustomerPortalBackend.service.HotelsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HotelsServiceImpl implements HotelsService {

    private final HotelRepository hotelRepository;
    private final AuthService authService;
    private final JwtService jwtService;
    private final RoomTypesRepository roomTypeRepository;
    private final RatePlanRepository ratePlansRepository;
    private final RoomRateMappingRepository roomRateMappingRepository;
    private final UserRoomRateMappingRepository userRoomRateMappingRepository;
    private final UserHotelRepository userHotelRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ApiResponse addHotelsToUser(AssignHotelsToUser assignHotelsToUser, String token) {
        Optional<Admin> optionalAdmin = authService.authenticateAdmin(token);
        if (optionalAdmin.isEmpty()) {
            return buildHotelErrorResponse("Invalid Token");
        }

        User user = userRepository.findByEmail(assignHotelsToUser.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid User"));

        if (assignHotelsToUser.hotels() == null || assignHotelsToUser.hotels().isEmpty()) {
            return buildHotelErrorResponse("Hotel list cannot be null or empty");
        }

        for (HotelsDTO hotelDto : assignHotelsToUser.hotels()) {
            Hotel hotel = resolveHotel(hotelDto.getId(), hotelDto.getName());
            boolean alreadyAssigned = userHotelRepository.existsByUserAndHotel(user, hotel);

            if (!alreadyAssigned) {
                UserHotel userHotel = UserHotel.builder()
                        .user(user)
                        .hotel(hotel)
                        .active(true)
                        .sellable(false)
                        .build();

                userHotelRepository.save(userHotel);
            }
        }

        return buildHotelSuccessResponse("Hotels added successfully");
    }

    @Override
    @Transactional
    public ApiResponse removeHotelFromUser(UserHotelRequest request, String token) {
        Optional<Admin> optionalAdmin = authService.authenticateAdmin(token);
        if (optionalAdmin.isEmpty()) {
            return buildHotelErrorResponse("Invalid Token");
        }

        UserHotel userHotel = resolveUserHotelForAdmin(
                request.userHotelId(),
                request.email(),
                request.hotelId(),
                request.hotelName()
        );

        if (userHotel.getUser() != null && userHotel.getUser().getUserHotels() != null) {
            userHotel.getUser().getUserHotels().removeIf(mapping -> mapping.getId().equals(userHotel.getId()));
        }

        if (userHotel.getHotel() != null && userHotel.getHotel().getUserHotels() != null) {
            userHotel.getHotel().getUserHotels().removeIf(mapping -> mapping.getId().equals(userHotel.getId()));
        }

        userHotelRepository.delete(userHotel);

        return buildHotelSuccessResponse("Hotel unmapped from user successfully");
    }

    @Override
    @Transactional
    public ApiResponse updateUserHotelSellable(ToggleUserHotelSellableRequest request, String token) {
        Optional<Admin> optionalAdmin = authService.authenticateAdmin(token);
        Optional<User> optionalUser = Optional.empty();

        if (optionalAdmin.isEmpty()) {
            optionalUser = authService.authenticateUser(token);
        }

        if (optionalAdmin.isEmpty() && optionalUser.isEmpty()) {
            return buildHotelErrorResponse("Invalid Token");
        }

        UserHotel userHotel;

        if (optionalAdmin.isPresent()) {
            userHotel = resolveUserHotelForAdmin(
                    request.userHotelId(),
                    request.email(),
                    request.hotelId(),
                    request.hotelName()
            );
        } else {
            User user = optionalUser.get();
            userHotel = resolveUserHotelForUser(user, request.userHotelId(), request.hotelId(), request.hotelName());
        }

        Boolean newSellableValue = request.sellable() == null
                ? !Boolean.TRUE.equals(userHotel.getSellable())
                : request.sellable();

        userHotel.setSellable(newSellableValue);
        userHotelRepository.save(userHotel);

        return ApiResponse.builder()
                .success(true)
                .message("Hotel sellable status updated successfully")
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public ApiResponse getListOfHotelsOnlineByNames(String names, String token) {
        if (!jwtService.isAccessToken(token)) {
            return buildHotelErrorResponse("Invalid token");
        }
        Optional<List<Hotel>> optionalHotels = hotelRepository.findByNameContainingIgnoreCase(names);
        if (optionalHotels.isPresent()) {
            return buildHotelSuccessResponse("Hotels found");
        }
        return buildHotelResponse(optionalHotels);
    }

    @Override
    public ApiDataResponse<List<RoomTypeDTO>> getRoomType(String hotelCode, String token) {
        Optional<List<Hotel>> hotels = hotelRepository.findByNameContainingIgnoreCase(hotelCode);
        if (hotels.isEmpty() || hotels.get().isEmpty()) {
            return ApiDataResponse.<List<RoomTypeDTO>>builder()
                    .success(false)
                    .message("No hotels found with the specified name")
                    .timestamp(Instant.now())
                    .data(List.of())
                    .build();
        }
        List<RoomTypeDTO> roomTypes = hotels.get().stream()
                .flatMap(hotel -> hotel.getRoomTypes().stream())
                .map(this::mapToRoomTypeDTO)
                .toList();

        return ApiDataResponse.<List<RoomTypeDTO>>builder()
                .success(true)
                .message("Room Types fetched successfully")
                .data(roomTypes)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public ApiDataResponse<List<RatePlanDTO>> getRatePlan(String roomTypeCode, String token) {
        RoomType roomType = roomTypeRepository.findByRoomTypeCode(roomTypeCode)
                .orElseThrow(() -> new RuntimeException("Room Type not found"));

        List<RatePlanDTO> ratePlans = roomRateMappingRepository
                .findByRoomType(roomType).stream()
                .map(RoomRateMapping::getRatePlan)
                .map(this::mapToRatePlanDTO)
                .toList();
        if (ratePlans.isEmpty()) {
            throw new RuntimeException("No rate plans found");
        }
        return ApiDataResponse.<List<RatePlanDTO>>builder()
                .success(true)
                .message("Rate Plan fetched successfully")
                .data(ratePlans)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public ApiDataResponse<List<HotelsDTO>> getHotelDetailsWithRoomTypeAndRatePlan(String hotelCode, String token) {
        Optional<List<Hotel>> hotels = hotelRepository.findByNameContainingIgnoreCase(hotelCode);
        if (hotels.isEmpty() || hotels.get().isEmpty()) {
            return ApiDataResponse.<List<HotelsDTO>>builder()
                    .success(false)
                    .message("No hotels found with the specified name")
                    .timestamp(Instant.now())
                    .data(List.of())
                    .build();
        }
        return ApiDataResponse.<List<HotelsDTO>>builder()
                .success(true)
                .message("Hotel details fetched successfully")
                .timestamp(Instant.now())
                .data(hotels.get().stream().map(this::mapToDTO).toList())
                .build();
    }

    @Override
    public ApiDataResponse<List<HotelsDTO>> seeAllHotels(String token) {
        Optional<UserDetails> userDetails = authService.authenticateUserDetails(token);
        if (userDetails.isEmpty()) {
            return ApiDataResponse.<List<HotelsDTO>>builder()
                    .success(false)
                    .message("Invalid token")
                    .timestamp(Instant.now())
                    .data(List.of())
                    .build();
        }
        List<Hotel> hotels = hotelRepository.findAll();
        List<HotelsDTO> hotelsDTOList = hotels.stream().map(this::mapToDTO).toList();
        return ApiDataResponse.<List<HotelsDTO>>builder()
                .success(true)
                .message("Hotels fetched successfully")
                .timestamp(Instant.now())
                .data(hotelsDTOList)
                .build();
    }

    @Override
    public ApiDataResponse<List<RoomType>> getAllRoomTypes(String token) {
        Optional<UserDetails> userDetails = authService.authenticateUserDetails(token);
        if (userDetails.isEmpty()) {
            return ApiDataResponse.<List<RoomType>>builder()
                    .success(false)
                    .message("Invalid token")
                    .timestamp(Instant.now())
                    .data(List.of())
                    .build();
        }
        return ApiDataResponse.<List<RoomType>>builder()
                .success(true)
                .message("Room types fetched successfully")
                .timestamp(Instant.now())
                .data(roomTypeRepository.findAll())
                .build();
    }

    @Override
    public ApiDataResponse<List<RatePlan>> getAllRatePlans(String token) {
        Optional<UserDetails> userDetails = authService.authenticateUserDetails(token);
        if (userDetails.isEmpty()) {
            return ApiDataResponse.<List<RatePlan>>builder()
                    .success(false)
                    .message("Invalid token")
                    .timestamp(Instant.now())
                    .data(List.of())
                    .build();
        }
        return ApiDataResponse.<List<RatePlan>>builder()
                .success(true)
                .message("Rate plans fetched successfully")
                .timestamp(Instant.now())
                .data(ratePlansRepository.findAll())
                .build();
    }

    @Override
    public ApiDataResponse<List<RoomRateMapping>> getAllRoomRateMapping(String token) {
        return ApiDataResponse.<List<RoomRateMapping>>builder()
                .success(true)
                .message("Room rate mapping fetched successfully")
                .timestamp(Instant.now())
                .data(roomRateMappingRepository.findAll())
                .build();
    }

    @Override
    @Transactional
    public ApiDataResponse<List<RoomRateMappingResponse>> roomTypeRatePlanMappingByUser(
            RoomRateTypeMappingRequestByUser request,
            String token
    ) {
        Optional<User> optionalUser = authService.authenticateUser(token);
        if (optionalUser.isEmpty()) {
            return ApiDataResponse.<List<RoomRateMappingResponse>>builder()
                    .success(false)
                    .message("User not authenticated")
                    .timestamp(Instant.now())
                    .data(null)
                    .build();
        }
        Hotel hotel = hotelRepository.findByNameIgnoreCase(request.hotelName())
                .orElseThrow(() -> new RuntimeException("Hotel not found"));
        User user = optionalUser.get();
        if (!userHotelRepository.existsByUserAndHotel(user, hotel)) {
            throw new BadCredentialsException("User is not assigned to the selected hotel");
        }
        RoomType roomType = roomTypeRepository.findByRoomTypeCode(request.roomTypeCode())
                .orElseThrow(() -> new RuntimeException("Room type Code not found"));

        if (!roomType.getHotel().getId().equals(hotel.getId())) {
            throw new IllegalArgumentException("Room Type does not belong to selected Hotel");
        }
        RatePlan ratePlan = ratePlansRepository.findByHotelAndRatePlanCode(hotel, request.ratePlanCode())
                .orElseThrow(() -> new RuntimeException("No Rate Plan found for Hotel"));

        if (!ratePlan.getHotel().getId().equals(hotel.getId())) {
            throw new IllegalArgumentException("Rate Plan does not belong to selected Hotel");
        }

        List<RoomRateMapping> mappings = roomRateMappingRepository.findByRoomTypeAndRatePlan(roomType, ratePlan);
        if (mappings.isEmpty()) {
            RoomRateMapping newMapping = RoomRateMapping.builder()
                    .roomType(roomType)
                    .ratePlan(ratePlan)
                    .baseRate(BigDecimal.ZERO)
                    .taxesAndFee(BigDecimal.ZERO)
                    .feeCollectedByHotel(BigDecimal.ZERO)
                    .totalTripCost(BigDecimal.ZERO)
                    .agentEarningsPercent(BigDecimal.ZERO)
                    .hotelReceives(BigDecimal.ZERO)
                    .occupancy(roomType.getBaseOccupancy())
                    .cancellationCharge(BigDecimal.ZERO)
                    .breakfastIncluded(false)
                    .active(true)
                    .build();

            RoomRateMapping savedMapping = roomRateMappingRepository.save(newMapping);
            UserRoomRateMapping userMapping = UserRoomRateMapping.builder()
                    .user(user)
                    .roomRateMapping(savedMapping)
                    .active(true)
                    .build();
            userRoomRateMappingRepository.save(userMapping);
            mappings = List.of(savedMapping);
        } else {
            for (RoomRateMapping mapping : mappings) {
                boolean exists = userRoomRateMappingRepository.existsByUserAndRoomRateMapping(user, mapping);
                if (!exists) {
                    UserRoomRateMapping userMapping = UserRoomRateMapping.builder()
                            .user(user)
                            .roomRateMapping(mapping)
                            .active(true)
                            .build();
                    userRoomRateMappingRepository.save(userMapping);
                }
            }
        }

        List<RoomRateMappingResponse> response = mappings.stream()
                .map(this::mapToRoomRateMappingResponse)
                .toList();
        return ApiDataResponse.<List<RoomRateMappingResponse>>builder()
                .success(true)
                .message("Room Rate Mapping Done successfully")
                .data(response)
                .timestamp(Instant.now())
                .build();
    }

    private UserHotel resolveUserHotelForAdmin(UUID userHotelId, String email, UUID hotelId, String hotelName) {
        if (userHotelId != null) {
            UserHotel userHotel = userHotelRepository.findById(userHotelId)
                    .orElseThrow(() -> new RuntimeException("User hotel mapping not found"));

            if (email != null && !email.isBlank() && !userHotel.getUser().getEmail().equalsIgnoreCase(email)) {
                throw new BadCredentialsException("This userHotelId does not belong to the given user email");
            }

            return userHotel;
        }

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required when userHotelId is not provided");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid User"));
        Hotel hotel = resolveHotel(hotelId, hotelName);

        return userHotelRepository.findByUserAndHotel(user, hotel)
                .orElseThrow(() -> new RuntimeException("Hotel is not mapped to this user"));
    }

    private UserHotel resolveUserHotelForUser(User user, UUID userHotelId, UUID hotelId, String hotelName) {
        if (userHotelId != null) {
            UserHotel userHotel = userHotelRepository.findById(userHotelId)
                    .orElseThrow(() -> new RuntimeException("User hotel mapping not found"));

            if (!userHotel.getUser().getId().equals(user.getId())) {
                throw new BadCredentialsException("You cannot update another user's hotel mapping");
            }

            return userHotel;
        }

        Hotel hotel = resolveHotel(hotelId, hotelName);
        return userHotelRepository.findByUserAndHotel(user, hotel)
                .orElseThrow(() -> new RuntimeException("Hotel is not mapped to this user"));
    }

    private Hotel resolveHotel(UUID hotelId, String hotelName) {
        if (hotelId != null) {
            return hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + hotelId));
        }

        if (hotelName == null || hotelName.isBlank()) {
            throw new RuntimeException("Either hotelId or hotelName is required");
        }

        return hotelRepository.findByNameIgnoreCase(hotelName)
                .orElseThrow(() -> new RuntimeException("Hotel not found: " + hotelName));
    }

    private RoomRateMappingResponse mapToRoomRateMappingResponse(RoomRateMapping mapping) {
        return RoomRateMappingResponse.builder()
                .roomRateMappingId(mapping.getId())
                .hotelName(mapping.getRoomType().getHotel().getName())
                .roomTypeCode(mapping.getRoomType().getRoomTypeCode())
                .roomTypeName(mapping.getRoomType().getRoomTypeName())
                .ratePlanCode(mapping.getRatePlan().getRatePlanCode())
                .ratePlanName(mapping.getRatePlan().getRatePlanName())
                .baseRate(mapping.getBaseRate())
                .taxesAndFee(mapping.getTaxesAndFee())
                .feeCollectedByHotel(mapping.getFeeCollectedByHotel())
                .totalTripCost(mapping.getTotalTripCost())
                .agentEarningsPercent(mapping.getAgentEarningsPercent())
                .hotelReceives(mapping.getHotelReceives())
                .occupancy(mapping.getOccupancy())
                .cancellationCharge(mapping.getCancellationCharge())
                .payLaterDeadline(mapping.getPayLaterDeadline())
                .breakfastIncluded(mapping.getBreakfastIncluded())
                .active(mapping.getActive())
                .build();
    }

    private ApiResponse buildHotelResponse(Optional<List<Hotel>> optionalHotels) {
        if (optionalHotels.isEmpty() || optionalHotels.get().isEmpty()) {
            return buildHotelErrorResponse("No hotels found with the specified name");
        }
        return buildHotelSuccessResponse("Hotels fetched successfully");
    }

    private HotelsDTO mapToDTO(Hotel hotel) {
        return HotelsDTO.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .active(hotel.getActive())
                .hotelActive(hotel.getActive())
                .roomTypes(hotel.getRoomTypes() == null ? List.of() :
                        hotel.getRoomTypes().stream().map(this::mapToRoomTypeDTO).toList())
                .ratePlans(hotel.getRatePlans() == null ? List.of() :
                        hotel.getRatePlans().stream().map(this::mapToRatePlanDTO).toList())
                .build();
    }

    private RoomTypeDTO mapToRoomTypeDTO(RoomType roomType) {
        return RoomTypeDTO.builder()
                .roomTypeCode(roomType.getRoomTypeCode())
                .roomTypeName(roomType.getRoomTypeName())
                .shortDescription(roomType.getShortDescription())
                .longDescription(roomType.getLongDescription())
                .roomCategory(roomType.getRoomCategory())
                .maxRoomCapacity(roomType.getMaxRoomCapacity())
                .maxAdults(roomType.getMaxAdults())
                .maxChildren(roomType.getMaxChildren())
                .baseOccupancy(roomType.getBaseOccupancy())
                .roomView(roomType.getRoomView())
                .roomSize(roomType.getRoomSize())
                .units(roomType.getUnits())
                .bedType(roomType.getBedType())
                .smokingAllowed(roomType.getSmokingAllowed())
                .build();
    }

    private RatePlanDTO mapToRatePlanDTO(RatePlan ratePlan) {
        return RatePlanDTO.builder()
                .ratePlanCode(ratePlan.getRatePlanCode())
                .ratePlanName(ratePlan.getRatePlanName())
                .bookingStartDate(ratePlan.getBookingStartDate())
                .bookingEndDate(ratePlan.getBookingEndDate())
                .sellingStartDate(ratePlan.getSellingStartDate())
                .sellingEndDate(ratePlan.getSellingEndDate())
                .shortDescription(ratePlan.getShortDescription())
                .longDescription(ratePlan.getLongDescription())
                .rateType(ratePlan.getRateType())
                .rateCategory(ratePlan.getRateCategory())
                .marketCode(ratePlan.getMarketCode())
                .sourceCode(ratePlan.getSourceCode())
                .build();
    }

    private ApiResponse buildHotelSuccessResponse(String message) {
        return ApiResponse.builder()
                .success(true)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    private ApiResponse buildHotelErrorResponse(String message) {
        return ApiResponse.builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
