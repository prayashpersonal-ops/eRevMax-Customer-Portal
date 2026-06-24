package com.example.CustomerPortalBackend.service.implementations;

import com.example.CustomerPortalBackend.entity.Admin;
import com.example.CustomerPortalBackend.entity.RefreshToken;
import com.example.CustomerPortalBackend.entity.User;
import com.example.CustomerPortalBackend.enums.UserStatus;
import com.example.CustomerPortalBackend.api.request.RefreshTokenRequest;
import com.example.CustomerPortalBackend.repository.AdminRepository;
import com.example.CustomerPortalBackend.repository.RefreshTokenRepository;
import com.example.CustomerPortalBackend.repository.UserRepository;
import com.example.CustomerPortalBackend.security.CookieService;
import com.example.CustomerPortalBackend.security.JwtService;
import com.example.CustomerPortalBackend.service.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final CookieService cookieService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;

    @Override
    //Read data from the refresh token from the body
    public Optional<String> readRefreshTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request){
        if (request.getCookies()!=null){
            Optional<String> fromCookie= Arrays.stream(request.getCookies())
                    .filter(c-> cookieService.getRefreshTokenCookieName().equals(c.getName()))
                    .map(Cookie::getValue)
                    .filter(v -> v!=null && !v.isBlank()).findFirst();
            if (fromCookie.isPresent()){
                return fromCookie;
            }
        }
        //2. Body
        if (body!=null && body.refreshToken()!=null && !body.refreshToken().isBlank()){
            return Optional.of(body.refreshToken().trim());
        }
        //3. Custome Header
        String refreshHeader = request.getHeader("X-Refresh-Token");
        if (refreshHeader!=null && !refreshHeader.isBlank()){
            return Optional.of(refreshHeader.trim());
        }
        //Authorization = Bearer <token>
        String authHeader =  request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader!=null && !authHeader.regionMatches(true,0,"Bearer ",0,7)) {
            String candidate = authHeader.substring(7).trim();
            if (!candidate.isEmpty()) {
                try {
                    if (jwtService.isRefreshToken(candidate)) {
                        return Optional.of(candidate);
                    }else {
                        return Optional.empty();
                    }
                } catch (Exception ignored) {
                    throw new BadCredentialsException("Unauthorized: "+ignored.getMessage());
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public RefreshToken refreshTokenValidity(String refreshToken){
        if (!jwtService.isRefreshToken(refreshToken)){
            throw new BadCredentialsException("Invalid Refresh Token");
        }
        String jti = jwtService.getJwtId(refreshToken);
        RefreshToken storedRefreshToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new BadCredentialsException("Refresh Token is not Recognized"));
        if (storedRefreshToken.getRevoked()) {
            throw new BadCredentialsException("Refresh Token isRevoked");
        }
        return storedRefreshToken;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        readRefreshTokenFromRequest(null, request).ifPresent(token -> {
            try {
                if (jwtService.isRefreshToken(token)) {
                    String jti = jwtService.getJwtId(token);
                    refreshTokenRepository.findByJti(jti).ifPresent(rt -> {
                        rt.setRevoked(true);
                        refreshTokenRepository.save(rt);
                    });
                }
            } catch (JwtException ignored) {
            }
        });
        // Use CookieUtil (same behavior)
        cookieService.clearRefreshCookie(response);
        cookieService.addNoStoreHeaders(response);
        SecurityContextHolder.clearContext();
    }

    @Override
    public Optional<Admin> authenticateAdmin(String token) {
        try {
            if (token == null || !token.startsWith("Bearer ") ||
                    !jwtService.isAccessToken(token.substring(7))){
                return Optional.empty();
            }
            token = token.substring(7);
            String email = jwtService.extractUserEmail(token);
            
            return adminRepository.findByEmail(email);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> authenticateUser(String token) {
        try {
            if (token == null || !token.startsWith("Bearer ") ||
                    !jwtService.isAccessToken(token.substring(7))) {
                return Optional.empty();
            }
            token = token.substring(7);
            String email = jwtService.extractUserEmail(token);
            Optional<User> optionalUser = userRepository.findByEmail(email);
            if (optionalUser.isEmpty()) {
                return Optional.empty();
            }
            User user = optionalUser.get();
            if (!Boolean.TRUE.equals(user.getEnable()) || user.getStatus() != UserStatus.APPROVED) {
                return Optional.empty();
            }
            return optionalUser;
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserDetails> authenticateUserDetails(String token) {
        Optional<User> user = authenticateUser(token);
        Optional<Admin> admin = authenticateAdmin(token);
        if (user.isPresent()) {
            return Optional.of(user.get());
        } else if (admin.isPresent()) {
            return Optional.of(admin.get());
        }
        return Optional.empty();
    }


}
