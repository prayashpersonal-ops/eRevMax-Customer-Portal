package com.example.CustomerPortalBackend.security;

import com.example.CustomerPortalBackend.entity.RefreshToken;
import com.example.CustomerPortalBackend.entity.User;
import com.example.CustomerPortalBackend.enums.Provider;
import com.example.CustomerPortalBackend.enums.Role;
import com.example.CustomerPortalBackend.repository.RefreshTokenRepository;
import com.example.CustomerPortalBackend.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.auth.frontend.success-url}")
    private String frontendSuccessUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        logger.info("Successfully authenticated");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        if (oAuth2User == null) {
            logger.error("OAuth2User attributes are empty");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid OAuth2 user information");
            return;
        }

        String registrationID = "unknown";
        if (authentication instanceof OAuth2AuthenticationToken token) {
            registrationID = token.getAuthorizedClientRegistrationId();
        }

        logger.info("Registration ID: {}", registrationID);

        User user;
        switch (registrationID) {
            case "google" -> {
                String googleId = oAuth2User.getAttributes().getOrDefault("sub", "").toString();
                String email = oAuth2User.getAttributes().getOrDefault("email", "").toString();
                String name = oAuth2User.getAttributes().getOrDefault("name", "").toString();

                if (email.isBlank()) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not found from Google account");
                    return;
                }

                User newUser = User.builder()
                        .email(email)
                        .name(name)
                        .enable(false)
                        .provider(Provider.GOOGLE)
                        .role(Role.USER)
                        .providerId(googleId)
                        .build();

                user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(newUser));
            }
            default -> throw new RuntimeException("Invalid registration ID");
        }

        RefreshToken refreshTokenOb = refreshTokenRepository.getRefreshTokenByUser(user);
        if (refreshTokenOb == null) {
            refreshTokenOb = new RefreshToken();
            refreshTokenOb.setUser(user);
        }

        String jti = UUID.randomUUID().toString();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshedToken(user, jti);

        refreshTokenOb.setJti(jti);
        refreshTokenOb.setRevoked(false);
        refreshTokenOb.setAccessToken(accessToken);
        refreshTokenOb.setReplacedByToken(refreshToken);
        refreshTokenOb.setCreatedAt(Date.from(Instant.now()));
        refreshTokenOb.setExpiresAt(Date.from(Instant.now().plusSeconds(jwtService.getRefreshTokenValiditySeconds())));
        refreshTokenRepository.save(refreshTokenOb);

        cookieService.attachRefreshCookie(response, refreshToken, (int) jwtService.getRefreshTokenValiditySeconds());

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendSuccessUrl)
                .queryParam("token", accessToken)
                .build()
                .encode()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
