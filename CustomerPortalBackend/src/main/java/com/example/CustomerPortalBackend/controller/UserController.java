package com.example.CustomerPortalBackend.controller;


import com.example.CustomerPortalBackend.api.request.*;
import com.example.CustomerPortalBackend.api.response.ApiDataResponse;
import com.example.CustomerPortalBackend.dto.HotelsDTO;
import com.example.CustomerPortalBackend.dto.UserDTO;
import com.example.CustomerPortalBackend.api.response.ApiResponse;
import com.example.CustomerPortalBackend.api.response.TokenResponse;
import com.example.CustomerPortalBackend.service.UserService;
import com.nimbusds.oauth2.sdk.util.singleuse.AlreadyUsedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("register")
    public ResponseEntity<ApiResponse> registerUser(@RequestBody UserDTO userDTO){
        return ResponseEntity.ok().body(userService.createUser(userDTO));
    }

    @PostMapping("login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody LoginRequest loginRequest, HttpServletResponse response){
        return ResponseEntity.ok().body(userService.loginUser(loginRequest,response));
    }

    @PostMapping("refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request, HttpServletResponse response){
        return ResponseEntity.ok().body(userService.refreshToken(refreshTokenRequest, request, response));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("listOfHotelsOfUser")
    public ResponseEntity<ApiDataResponse<List<HotelsDTO>>> getListOfHotelsTheUserHaveByEmail( @RequestHeader(HttpHeaders.AUTHORIZATION) String token){
        return ResponseEntity.ok().body(userService.getListOfHotelsTheUserHave(token));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @PostMapping("searchListOfHotelsByUser")
    public ResponseEntity<ApiDataResponse<List<HotelsDTO>>> searchListOfHotelsByUser(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody SearchHotelsRequest searchHotelsRequest
    ){
        return ResponseEntity.ok().body(userService.searchListOfHotelsByUser(token,searchHotelsRequest));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("toggleListOfHotelsByUserSellable")
    public ResponseEntity<ApiDataResponse<List<HotelsDTO>>> toggleListOfHotelsByUserSellable(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody SearchHotelsRequest searchHotelsRequest
    ) throws AlreadyUsedException {
        return ResponseEntity.ok().body(userService.toggleListOfHotelsByUserSellable(searchHotelsRequest,token));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response){
        userService.logout(request, response);//Here can we do it logout with both refresh and access token?
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
