package com.renuka.notification_backend.auth.otp;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpCodeGenerator {

    private static final int OTP_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        return String.format("%06d", secureRandom.nextInt(OTP_BOUND));
    }
}
