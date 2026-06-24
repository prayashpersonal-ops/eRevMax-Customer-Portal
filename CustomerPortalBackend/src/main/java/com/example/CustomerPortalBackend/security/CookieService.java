package com.example.CustomerPortalBackend.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@Getter
public class CookieService {
    private final String refreshTokenCookieName;
    private final boolean cookieHttpOnly;
    private final boolean cookieSecure;
    private final String cookieDomain;
    private final String cookieSameSite;
    private final Logger  logger = LoggerFactory.getLogger(CookieService.class);

    public CookieService(@Value("${security.jwt.refresh-token-cookie-name}") String refreshTokenCookieName,
                         @Value("${security.jwt.cookie-http-only}") boolean cookieHttpOnly,
                         @Value("${security.jwt.cookie-secure}") boolean cookieSecure,
                         @Value("${security.jwt.cookie-domain}") String cookieDomain,
                         @Value("${security.jwt.cookie-same-site}") String cookieSameSite
    ) {
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.cookieHttpOnly = cookieHttpOnly;
        this.cookieSecure = cookieSecure;
        this.cookieDomain = cookieDomain;
        this.cookieSameSite = cookieSameSite;
    }
    //Create method to attach cookie to response
    public void attachRefreshCookie(HttpServletResponse response,String value,int maxAge){
        /*logger.info("Attaching refresh token cookie: name={}, value={}",refreshTokenCookieName, value);*/
         var responseCookieBuilder = ResponseCookie.from(refreshTokenCookieName,value)
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .maxAge(maxAge)
                .path("/")
                .sameSite(cookieSameSite);
         if (cookieDomain!=null || cookieDomain.length()>0){
             responseCookieBuilder.domain(cookieDomain);
         }
         ResponseCookie responseCookie = responseCookieBuilder.build();
         response.addHeader(HttpHeaders.SET_COOKIE,responseCookie.toString());
    }

    //Clear Refresh Cookie
    public void clearRefreshCookie(HttpServletResponse response){
        var responseCookieBuilder = ResponseCookie.from(refreshTokenCookieName,"")
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .maxAge(0)
                .path("/")
                .sameSite(cookieSameSite);
        if (cookieDomain!=null || cookieDomain.length()>0){
            responseCookieBuilder.domain(cookieDomain);
        }
        ResponseCookie responseCookie = responseCookieBuilder.build();
        response.addHeader(HttpHeaders.SET_COOKIE,responseCookie.toString());
    }

    public void addNoStoreHeaders(HttpServletResponse response){
        response.setHeader(HttpHeaders.CACHE_CONTROL,"no-store");//Do NOT store this response anywhere (cache, disk, memory)
        response.setHeader(HttpHeaders.PRAGMA,"no-cache");//Always fetch fresh data from server
    }
}
