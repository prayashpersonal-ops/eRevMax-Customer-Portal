package com.example.CustomerPortalBackend.service;

import com.example.CustomerPortalBackend.api.request.LoginRequest;
import com.example.CustomerPortalBackend.api.request.RefreshTokenRequest;
import com.example.CustomerPortalBackend.api.request.ToggleHotelRequest;
import com.example.CustomerPortalBackend.api.response.ApiDataResponse;
import com.example.CustomerPortalBackend.api.response.ApiResponse;
import com.example.CustomerPortalBackend.api.response.TokenResponse;
import com.example.CustomerPortalBackend.api.response.UserPageAbleResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AdminService {

    ApiDataResponse<UserPageAbleResponse> seeAllUsers(
            String token,
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortOrder,
            String search
    );

    ApiResponse userAccessDeniedByEmail(String email, String token);

    ApiResponse userAccessGrantedByEmail(String email, String token);

    TokenResponse logInAdmin(LoginRequest loginRequest, HttpServletResponse response);

    TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest, HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);

    ApiResponse updateHotelStatus(ToggleHotelRequest request, String token);
}
