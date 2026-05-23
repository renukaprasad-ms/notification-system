package com.renuka.notification_backend.security.jwt;

import com.renuka.notification_backend.user.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Service
public class JwtCookieService {

    private final JwtService jwtService;
    private final String accessTokenCookieName;
    private final String refreshTokenCookieName;
    private final boolean secureCookie;
    private final String sameSite;

    public JwtCookieService(
            JwtService jwtService,
            @Value("${app.jwt.access-cookie-name:access_token}") String accessTokenCookieName,
            @Value("${app.jwt.refresh-cookie-name:refresh_token}") String refreshTokenCookieName,
            @Value("${app.jwt.cookie-secure:false}") boolean secureCookie,
            @Value("${app.jwt.cookie-same-site:Strict}") String sameSite
    ) {
        this.jwtService = jwtService;
        this.accessTokenCookieName = accessTokenCookieName;
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
    }

    public void addAuthCookies(HttpHeaders headers, User user) {
        headers.add(HttpHeaders.SET_COOKIE, accessTokenCookie(jwtService.createAccessToken(user)).toString());
        headers.add(HttpHeaders.SET_COOKIE, refreshTokenCookie(jwtService.createRefreshToken(user)).toString());
    }

    public void clearAuthCookies(HttpHeaders headers) {
        headers.add(HttpHeaders.SET_COOKIE, expiredCookie(accessTokenCookieName, "/").toString());
        headers.add(HttpHeaders.SET_COOKIE, expiredCookie(refreshTokenCookieName, "/").toString());
    }

    public Optional<String> getAccessToken(HttpServletRequest request) {
        return getCookieValue(request, accessTokenCookieName);
    }

    public Optional<String> getRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, refreshTokenCookieName);
    }

    private ResponseCookie accessTokenCookie(String token) {
        return baseCookie(accessTokenCookieName, token, "/")
                .maxAge(Duration.ofSeconds(jwtService.getAccessTokenExpirationSeconds()))
                .build();
    }

    private ResponseCookie refreshTokenCookie(String token) {
        return baseCookie(refreshTokenCookieName, token, "/")
                .maxAge(Duration.ofSeconds(jwtService.getRefreshTokenExpirationSeconds()))
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(path);
    }

    private ResponseCookie expiredCookie(String name, String path) {
        return baseCookie(name, "", path)
                .maxAge(Duration.ZERO)
                .build();
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
