package com.example.CustomerPortalBackend.controller;

import com.example.CustomerPortalBackend.api.request.AssignHotelsToUser;
import com.example.CustomerPortalBackend.api.request.CodeRequest;
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
import com.example.CustomerPortalBackend.service.HotelsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/hotels/")
@RequiredArgsConstructor
public class HotelsController {
    private final HotelsService hotelsService;

    @PostMapping("addHotel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> addHotel(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody AssignHotelsToUser assignHotelsToUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hotelsService.addHotelsToUser(assignHotelsToUser, token));
    }

    @PostMapping("removeHotel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> removeHotel(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody UserHotelRequest request) {
        return ResponseEntity.ok(hotelsService.removeHotelFromUser(request, token));
    }

    @PostMapping("updateUserHotelSellable")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse> updateUserHotelSellable(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody ToggleUserHotelSellableRequest request) {
        return ResponseEntity.ok(hotelsService.updateUserHotelSellable(request, token));
    }

    @GetMapping("listOfHotelsByName")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse> getListOfHotelsOnlineByNames(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody String names) {
        return ResponseEntity.ok().body(hotelsService.getListOfHotelsOnlineByNames(names, token));
    }

    @PostMapping("getRoomType")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiDataResponse<List<RoomTypeDTO>>> getRoomType(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody CodeRequest request) {
        return ResponseEntity.ok(hotelsService.getRoomType(request.code(), token));
    }

    @PostMapping("getRatePlan")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiDataResponse<List<RatePlanDTO>>> getRatePlan(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody CodeRequest request) {
        return ResponseEntity.ok(hotelsService.getRatePlan(request.code(), token));
    }

    @PostMapping("getHotelDetailsWithRoomTypeAndPatePlan")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiDataResponse<List<HotelsDTO>>> getHotelDetailsWithRoomTypeAndPatePlan(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody CodeRequest request) {
        return ResponseEntity.ok(hotelsService.getHotelDetailsWithRoomTypeAndRatePlan(request.code(), token));
    }

    @GetMapping("seeAllHotels")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiDataResponse<List<HotelsDTO>>> seeAllHotels(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return ResponseEntity.ok(hotelsService.seeAllHotels(token));
    }

    @GetMapping("seeAllRoomTypes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiDataResponse<List<RoomType>>> seeAllRoomTypes(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return ResponseEntity.ok(hotelsService.getAllRoomTypes(token));
    }

    @GetMapping("seeAllRatePlans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiDataResponse<List<RatePlan>>> seeAllRatePlans(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return ResponseEntity.ok(hotelsService.getAllRatePlans(token));
    }

    @GetMapping("seeAllRoomRateMapping")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiDataResponse<List<RoomRateMapping>>> seeAllRoomRateMapping(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return ResponseEntity.ok(hotelsService.getAllRoomRateMapping(token));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("roomRateMappingPlan")
    public ResponseEntity<ApiDataResponse<List<RoomRateMappingResponse>>> roomTypeRatePlanMappingByUser(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody RoomRateTypeMappingRequestByUser roomRateTypeMappingByUser) {
        return ResponseEntity.ok().body(hotelsService.roomTypeRatePlanMappingByUser(roomRateTypeMappingByUser, token));
    }
}
