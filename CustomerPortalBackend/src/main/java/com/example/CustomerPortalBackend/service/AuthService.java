package com.example.CustomerPortalBackend.service;

import com.example.CustomerPortalBackend.entity.Admin;
import com.example.CustomerPortalBackend.entity.RefreshToken;
import com.example.CustomerPortalBackend.entity.User;
import com.example.CustomerPortalBackend.api.request.RefreshTokenRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public interface AuthService {

    Optional<String> readRefreshTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request);

    RefreshToken refreshTokenValidity(String refreshToken);

    void logout(HttpServletRequest request, HttpServletResponse response);

    Optional<Admin> authenticateAdmin(String token);

    Optional<User> authenticateUser(String token);

    Optional<UserDetails> authenticateUserDetails(String token);
}
