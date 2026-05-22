package com.renuka.notification_backend.auth.otp;

import com.renuka.notification_backend.common.exception.BadRequestException;
import com.renuka.notification_backend.user.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class OtpService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final OtpCodeGenerator otpCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final int expiryMinutes;
    private final int maxAttempts;

    public OtpService(
            OtpVerificationRepository otpVerificationRepository,
            OtpCodeGenerator otpCodeGenerator,
            PasswordEncoder passwordEncoder,
            @Value("${app.otp.expiry-minutes:5}") int expiryMinutes,
            @Value("${app.otp.max-attempts:5}") int maxAttempts
    ) {
        this.otpVerificationRepository = otpVerificationRepository;
        this.otpCodeGenerator = otpCodeGenerator;
        this.passwordEncoder = passwordEncoder;
        this.expiryMinutes = expiryMinutes;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public String createOtp(String destination, OtpPurpose purpose) {
        return createOtp(null, destination, purpose);
    }

    @Transactional
    public String createOtp(User user, String destination, OtpPurpose purpose) {
        validateCreateRequest(destination, purpose);

        String otp = otpCodeGenerator.generate();

        OtpVerification otpVerification = new OtpVerification();
        otpVerification.setUser(user);
        otpVerification.setDestination(destination.trim().toLowerCase());
        otpVerification.setChannel(OtpChannel.EMAIL);
        otpVerification.setPurpose(purpose);
        otpVerification.setOtpHash(passwordEncoder.encode(otp));
        otpVerification.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        otpVerification.setMaxAttempts(maxAttempts);

        otpVerificationRepository.save(otpVerification);
        return otp;
    }

    @Transactional
    public void verifyOtp(String destination, OtpPurpose purpose, String otp) {
        validateVerifyRequest(destination, purpose, otp);

        OtpVerification otpVerification = otpVerificationRepository
                .findTopByDestinationAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                        destination.trim().toLowerCase(),
                        purpose
                )
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        if (otpVerification.isExpired() || otpVerification.isConsumed()) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        if (otpVerification.isMaxAttemptsReached()) {
            throw new BadRequestException("Maximum OTP attempts exceeded");
        }

        if (!passwordEncoder.matches(otp, otpVerification.getOtpHash())) {
            otpVerification.incrementAttemptCount();
            otpVerificationRepository.save(otpVerification);
            throw new BadRequestException("Invalid OTP");
        }

        otpVerification.markVerified();
        otpVerification.markConsumed();
        otpVerificationRepository.save(otpVerification);
    }

    private void validateCreateRequest(String destination, OtpPurpose purpose) {
        if (!StringUtils.hasText(destination)) {
            throw new BadRequestException("OTP destination is required");
        }

        if (purpose == null) {
            throw new BadRequestException("OTP purpose is required");
        }
    }

    private void validateVerifyRequest(String destination, OtpPurpose purpose, String otp) {
        validateCreateRequest(destination, purpose);

        if (!StringUtils.hasText(otp)) {
            throw new BadRequestException("OTP is required");
        }
    }
}
