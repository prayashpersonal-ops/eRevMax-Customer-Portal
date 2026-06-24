package com.example.CustomerPortalBackend.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
@Data
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenValiditySeconds;
    private final long refreshTokenValiditySeconds;
    private final String issuer;

    public JwtService(@Value("${security.jwt.secret}") String secretKey,
                      @Value("${security.jwt.access-ttl-seconds}") long accessTokenValiditySeconds,
                      @Value("${security.jwt.refresh-ttl-seconds}") long refreshTokenValiditySeconds,
                      @Value("${security.jwt.issuer}") String issuer) {
        if (secretKey == null || secretKey.length() < 32) {//as HS256
            throw new IllegalArgumentException("Secret key must be at least 64 characters long");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        this.issuer = issuer;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }
    public String generateRefreshedToken(UserDetails user,String jti){
        Instant now = Instant.now();
        List<String> roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return Jwts.builder()
                .id(jti)
                .subject(user.getUsername())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenValiditySeconds)))
                .claim("typ","refresh")
                .claim("roles",roles)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUserEmail(String jwt) {
        return extractClaims(jwt, Claims::getSubject);
    }

    public String generateAccessToken(UserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", user.getAuthorities())
                .claim("enabled",user.isEnabled())
                .claim("typ", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenValiditySeconds)))
                .issuer(issuer)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    public boolean isValidToken(String token, UserDetails user) {
        return user.getUsername().equals(extractUserEmail(token))
                && notExpired(token)
                && user.isEnabled();
    }

    public boolean isAccessToken(String token){
        Claims c = parse(token).getPayload();
        return "access".equals(c.get("typ"));
    }

    public boolean notExpired(String token) {
        return extractClaims(token, Claims::getExpiration)
                .after(Date.from(Instant.now()));
    }
    public Jws<Claims> parse(String token){
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
    }

    public <T> T extractClaims(String jwt, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(jwt);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException ex) {
            throw new BadCredentialsException("Access token expired");

        } catch (JwtException ex) {
            throw new BadCredentialsException("Invalid token");
        }
    }

    public boolean isRefreshToken(String token){
        Claims c = parse(token).getPayload();
        return "refresh".equals(c.get("typ"));
    }

    public String getUserDetailsEmail(String token){
        Claims c = parse(token).getPayload();
        return  c.getSubject();
    }

    public String getJwtId(String token){
        return parse(token).getPayload().getId();
    }

    public List<String> getRoles(String token){
        Claims c = parse(token).getPayload();
        return c.get("roles", List.class);
    }

}