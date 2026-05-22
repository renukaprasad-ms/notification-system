package com.renuka.notification_backend.security.jwt;

import java.util.List;

public record JwtClaims(
        String subject,
        JwtTokenType tokenType,
        List<String> roles
) {
}
