package com.example.CustomerPortalBackend.service;

import com.example.CustomerPortalBackend.api.request.*;
import com.example.CustomerPortalBackend.api.response.ApiDataResponse;
import com.example.CustomerPortalBackend.dto.HotelsDTO;
import com.example.CustomerPortalBackend.dto.UserDTO;
import com.example.CustomerPortalBackend.api.response.ApiResponse;
import com.example.CustomerPortalBackend.api.response.TokenResponse;
import com.nimbusds.oauth2.sdk.util.singleuse.AlreadyUsedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface UserService {
    ApiResponse createUser(UserDTO userDTO);

    TokenResponse loginUser(LoginRequest loginRequest, HttpServletResponse response);

    ApiDataResponse<List<HotelsDTO>> getListOfHotelsTheUserHave(String token);

    ApiDataResponse<List<HotelsDTO>> searchListOfHotelsByUser(String token, SearchHotelsRequest searchHotelsRequest);

    ApiDataResponse<List<HotelsDTO>> toggleListOfHotelsByUserSellable(SearchHotelsRequest searchHotelsRequest, String token) throws AlreadyUsedException;

    TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest, HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);
}
