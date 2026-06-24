package com.example.CustomerPortalBackend.service;

import com.example.CustomerPortalBackend.api.request.AssignHotelsToUser;
import com.example.CustomerPortalBackend.api.request.RoomRateTypeMappingRequestByUser;
import com.example.CustomerPortalBackend.api.request.ToggleUserHotelSellableRequest;
import com.example.CustomerPortalBackend.api.request.UserHotelRequest;
import com.example.CustomerPortalBackend.api.response.ApiDataResponse;
import com.example.CustomerPortalBackend.api.response.ApiResponse;
import com.example.CustomerPortalBackend.api.response.RoomRateMappingResponse;
import com.example.CustomerPortalBackend.dto.HotelsDTO;
import com.example.CustomerPortalBackend.dto.RatePlanDTO;
import com.example.CustomerPortalBackend.dto.RoomTypeDTO;
import com.example.CustomerPortalBackend.entity.RatePlan;
import com.example.CustomerPortalBackend.entity.RoomRateMapping;
import com.example.CustomerPortalBackend.entity.RoomType;

import java.util.List;

public interface HotelsService {

    ApiResponse addHotelsToUser(AssignHotelsToUser assignHotelsToUser, String token);

    ApiResponse removeHotelFromUser(UserHotelRequest request, String token);

    ApiResponse updateUserHotelSellable(ToggleUserHotelSellableRequest request, String token);

    ApiResponse getListOfHotelsOnlineByNames(String names, String token);

    ApiDataResponse<List<RoomTypeDTO>> getRoomType(String hotelCode, String token);

    ApiDataResponse<List<RatePlanDTO>> getRatePlan(String roomTypeCode, String token);

    ApiDataResponse<List<HotelsDTO>> getHotelDetailsWithRoomTypeAndRatePlan(String hotelCode, String token);

    ApiDataResponse<List<HotelsDTO>> seeAllHotels(String token);

    ApiDataResponse<List<RoomType>> getAllRoomTypes(String token);

    ApiDataResponse<List<RatePlan>> getAllRatePlans(String token);

    ApiDataResponse<List<RoomRateMapping>> getAllRoomRateMapping(String token);

    ApiDataResponse<List<RoomRateMappingResponse>> roomTypeRatePlanMappingByUser(
            RoomRateTypeMappingRequestByUser roomRateTypeMappingByUser,
            String token
    );
}
