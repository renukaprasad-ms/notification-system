package com.renuka.notification_backend.auth.service;

import com.renuka.notification_backend.auth.dto.LoginRequest;
import com.renuka.notification_backend.auth.dto.LoginResponse;
import com.renuka.notification_backend.auth.dto.LoginType;
import com.renuka.notification_backend.auth.otp.OtpPurpose;
import com.renuka.notification_backend.auth.otp.OtpService;
import com.renuka.notification_backend.common.exception.BadRequestException;
import com.renuka.notification_backend.common.exception.UnauthorizedException;
import com.renuka.notification_backend.user.entity.User;
import com.renuka.notification_backend.user.repository.UserRepository;
import com.renuka.notification_backend.user.repository.UserRoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordHashService passwordHashService;
    private final OtpService otpService;

    public AuthService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            PasswordHashService passwordHashService,
            OtpService otpService
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordHashService = passwordHashService;
        this.otpService = otpService;
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        if (request.getLoginType() == LoginType.EMAIL_PASSWORD) {
            loginWithPassword(request, user);
        } else if (request.getLoginType() == LoginType.EMAIL_OTP) {
            loginWithOtp(request, user);
        } else {
            throw new BadRequestException("Unsupported login type");
        }

        return new LoginResult(user, toLoginResponse(user));
    }

    private void loginWithPassword(LoginRequest request, User user) {
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BadRequestException("Password is required");
        }

        if (!passwordHashService.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE);
        }

        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Email verification is required");
        }
    }

    private void loginWithOtp(LoginRequest request, User user) {
        if (!StringUtils.hasText(request.getOtp())) {
            throw new BadRequestException("OTP is required");
        }

        otpService.verifyOtp(user.getEmail(), OtpPurpose.LOGIN, request.getOtp());

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }
    }

    private LoginResponse toLoginResponse(User user) {
        List<String> roles = userRoleRepository.findByIdUserId(user.getId())
                .stream()
                .map(userRole -> userRole.getRole().getName().name())
                .toList();

        return new LoginResponse(user.getId(), user.getEmail(), user.getFullName(), roles);
    }

    public record LoginResult(User user, LoginResponse response) {
    }
}
