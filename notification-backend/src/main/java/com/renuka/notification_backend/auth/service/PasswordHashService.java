package com.renuka.notification_backend.auth.service;

import com.renuka.notification_backend.common.exception.BadRequestException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PasswordHashService {

    private final PasswordEncoder passwordEncoder;

    public PasswordHashService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hashPassword(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new BadRequestException("Password is required");
        }

        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String passwordHash) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(passwordHash)) {
            return false;
        }

        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
