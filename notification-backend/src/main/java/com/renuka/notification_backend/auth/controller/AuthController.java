package com.renuka.notification_backend.auth.controller;

import com.renuka.notification_backend.auth.dto.CreateUserRequest;
import com.renuka.notification_backend.auth.dto.ForgotPasswordOtpRequest;
import com.renuka.notification_backend.auth.dto.LoginOtpRequest;
import com.renuka.notification_backend.auth.dto.LoginRequest;
import com.renuka.notification_backend.auth.dto.LoginResponse;
import com.renuka.notification_backend.auth.dto.ResetPasswordRequest;
import com.renuka.notification_backend.auth.service.AuthService;
import com.renuka.notification_backend.common.response.ApiResponse;
import com.renuka.notification_backend.security.jwt.JwtCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<LoginResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        AuthService.LoginResult loginResult = authService.createUser(request);

        HttpHeaders headers = new HttpHeaders();
        jwtCookieService.addAuthCookies(headers, loginResult.user());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .headers(headers)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), loginResult.response(), "User created successfully"));
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

    @PostMapping("/login/otp")
    public ResponseEntity<ApiResponse<Void>> createLoginOtp(@Valid @RequestBody LoginOtpRequest request) {
        authService.createLoginOtp(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "Login OTP created successfully"));
    }

    @PostMapping("/forgot-password/otp")
    public ResponseEntity<ApiResponse<Void>> createPasswordResetOtp(
            @Valid @RequestBody ForgotPasswordOtpRequest request
    ) {
        authService.createPasswordResetOtp(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "Password reset OTP created successfully"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);

        HttpHeaders headers = new HttpHeaders();
        jwtCookieService.clearAuthCookies(headers);

        return ResponseEntity
                .status(HttpStatus.OK)
                .headers(headers)
                .body(ApiResponse.success(HttpStatus.OK.value(), "Password reset successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(HttpServletRequest request) {
        String refreshToken = jwtCookieService.getRefreshToken(request).orElse(null);
        AuthService.LoginResult loginResult = authService.refresh(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        jwtCookieService.addAuthCookies(headers, loginResult.user());

        return ResponseEntity
                .status(HttpStatus.OK)
                .headers(headers)
                .body(ApiResponse.success(HttpStatus.OK.value(), loginResult.response(), "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        HttpHeaders headers = new HttpHeaders();
        jwtCookieService.clearAuthCookies(headers);

        return ResponseEntity
                .status(HttpStatus.OK)
                .headers(headers)
                .body(ApiResponse.success(HttpStatus.OK.value(), "Logout successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> currentUser(Authentication authentication) {
        LoginResponse response = authService.currentUser(authentication.getName());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), response, "Profile fetched successfully"));
    }

    @GetMapping("/admin/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoginResponse>> currentAdmin(Authentication authentication) {
        LoginResponse response = authService.currentUser(authentication.getName());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), response, "Admin profile fetched successfully"));
    }
}
