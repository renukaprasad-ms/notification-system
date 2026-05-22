package com.renuka.notification_backend.auth.controller;

import com.renuka.notification_backend.auth.dto.LoginRequest;
import com.renuka.notification_backend.auth.dto.LoginResponse;
import com.renuka.notification_backend.auth.service.AuthService;
import com.renuka.notification_backend.common.response.ApiResponse;
import com.renuka.notification_backend.security.jwt.JwtCookieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtCookieService jwtCookieService;

    public AuthController(AuthService authService, JwtCookieService jwtCookieService) {
        this.authService = authService;
        this.jwtCookieService = jwtCookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult loginResult = authService.login(request);

        HttpHeaders headers = new HttpHeaders();
        jwtCookieService.addAuthCookies(headers, loginResult.user());

        return ResponseEntity
                .status(HttpStatus.OK)
                .headers(headers)
                .body(ApiResponse.success(HttpStatus.OK.value(), loginResult.response(), "Login successful"));
    }
}
