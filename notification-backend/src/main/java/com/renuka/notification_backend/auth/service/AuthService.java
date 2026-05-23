package com.renuka.notification_backend.auth.service;

import com.renuka.notification_backend.auth.dto.CreateUserRequest;
import com.renuka.notification_backend.auth.dto.ForgotPasswordOtpRequest;
import com.renuka.notification_backend.auth.dto.LoginOtpRequest;
import com.renuka.notification_backend.auth.dto.LoginRequest;
import com.renuka.notification_backend.auth.dto.LoginResponse;
import com.renuka.notification_backend.auth.dto.LoginType;
import com.renuka.notification_backend.auth.dto.ResetPasswordRequest;
import com.renuka.notification_backend.auth.otp.OtpDeliveryService;
import com.renuka.notification_backend.auth.otp.OtpPurpose;
import com.renuka.notification_backend.auth.otp.OtpService;
import com.renuka.notification_backend.common.utils.RedisRateLimitService;
import com.renuka.notification_backend.common.exception.BadRequestException;
import com.renuka.notification_backend.common.exception.ConflictException;
import com.renuka.notification_backend.common.exception.UnauthorizedException;
import com.renuka.notification_backend.security.jwt.JwtClaims;
import com.renuka.notification_backend.security.jwt.JwtService;
import com.renuka.notification_backend.security.jwt.JwtTokenType;
import com.renuka.notification_backend.user.entity.Role;
import com.renuka.notification_backend.user.entity.RoleName;
import com.renuka.notification_backend.user.entity.User;
import com.renuka.notification_backend.user.entity.UserRole;
import com.renuka.notification_backend.user.repository.RoleRepository;
import com.renuka.notification_backend.user.repository.UserRepository;
import com.renuka.notification_backend.user.repository.UserRoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordHashService passwordHashService;
    private final OtpService otpService;
    private final OtpDeliveryService otpDeliveryService;
    private final JwtService jwtService;
    private final RedisRateLimitService redisRateLimitService;
    private final long loginOtpLimit;
    private final long passwordResetOtpLimit;
    private final Duration otpRateLimitWindow;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordHashService passwordHashService,
            OtpService otpService,
            OtpDeliveryService otpDeliveryService,
            JwtService jwtService,
            RedisRateLimitService redisRateLimitService,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.otp.login.limit:5}") long loginOtpLimit,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.otp.password-reset.limit:5}") long passwordResetOtpLimit,
            @org.springframework.beans.factory.annotation.Value("${app.rate-limit.otp.window-minutes:15}") long otpRateLimitWindowMinutes
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordHashService = passwordHashService;
        this.otpService = otpService;
        this.otpDeliveryService = otpDeliveryService;
        this.jwtService = jwtService;
        this.redisRateLimitService = redisRateLimitService;
        this.loginOtpLimit = loginOtpLimit;
        this.passwordResetOtpLimit = passwordResetOtpLimit;
        this.otpRateLimitWindow = Duration.ofMinutes(otpRateLimitWindowMinutes);
    }

    @Transactional
    public LoginResult createUser(CreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setFullName(request.getFullName().trim());
        user.setPasswordHash(passwordHashService.hashPassword(request.getPassword()));
        user.setEmailVerified(true);
        user.setActive(true);

        User savedUser = userRepository.save(user);
        Role userRole = getOrCreateUserRole();
        userRoleRepository.save(new UserRole(savedUser, userRole));

        return new LoginResult(savedUser, toLoginResponse(savedUser));
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

    @Transactional
    public void createLoginOtp(LoginOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        redisRateLimitService.assertAllowed(
                "rate-limit:otp:login",
                email,
                loginOtpLimit,
                otpRateLimitWindow,
                "Too many login OTP requests. Please try again later."
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        String otp = otpService.createOtp(user, user.getEmail(), OtpPurpose.LOGIN);
        otpDeliveryService.sendOtp(user.getEmail(), OtpPurpose.LOGIN, otp);
    }

    @Transactional
    public void createPasswordResetOtp(ForgotPasswordOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        redisRateLimitService.assertAllowed(
                "rate-limit:otp:password-reset",
                email,
                passwordResetOtpLimit,
                otpRateLimitWindow,
                "Too many password reset OTP requests. Please try again later."
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email"));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        String otp = otpService.createOtp(user, user.getEmail(), OtpPurpose.PASSWORD_RESET);
        otpDeliveryService.sendOtp(user.getEmail(), OtpPurpose.PASSWORD_RESET, otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid password reset request"));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        otpService.verifyOtp(user.getEmail(), OtpPurpose.PASSWORD_RESET, request.getOtp());
        user.setPasswordHash(passwordHashService.hashPassword(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public LoginResult refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new UnauthorizedException("Refresh token is required");
        }

        JwtClaims claims = jwtService.validateToken(refreshToken, JwtTokenType.REFRESH)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        User user = userRepository.findByEmail(claims.subject())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        return new LoginResult(user, toLoginResponse(user));
    }

    @Transactional
    public LoginResponse currentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authentication is required"));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        return toLoginResponse(user);
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

    private Role getOrCreateUserRole() {
        return roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.USER);
                    role.setDescription("Default application user");
                    return roleRepository.save(role);
                });
    }

    public record LoginResult(User user, LoginResponse response) {
    }
}
