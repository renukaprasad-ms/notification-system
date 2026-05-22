package com.renuka.notification_backend.security.jwt;

import com.renuka.notification_backend.user.entity.User;
import com.renuka.notification_backend.user.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String JWT_ALGORITHM = "HS256";
    private static final int MIN_SECRET_LENGTH = 32;

    private final ObjectMapper objectMapper;
    private final UserRoleRepository userRoleRepository;
    private final String jwtSecret;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            UserRoleRepository userRoleRepository,
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.access-token-expiration-seconds:900}") long accessTokenExpirationSeconds,
            @Value("${app.jwt.refresh-token-expiration-seconds:604800}") long refreshTokenExpirationSeconds
    ) {
        if (jwtSecret == null || jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT secret must be at least 32 characters");
        }

        this.objectMapper = objectMapper;
        this.userRoleRepository = userRoleRepository;
        this.jwtSecret = jwtSecret;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    public String createAccessToken(User user) {
        return createToken(user, JwtTokenType.ACCESS, accessTokenExpirationSeconds);
    }

    public String createRefreshToken(User user) {
        return createToken(user, JwtTokenType.REFRESH, refreshTokenExpirationSeconds);
    }

    public Optional<JwtClaims> validateToken(String token, JwtTokenType expectedTokenType) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !isSignatureValid(parts[0], parts[1], parts[2])) {
                return Optional.empty();
            }

            Map<String, Object> claims = readJson(parts[1]);
            String subject = (String) claims.get("sub");
            String tokenTypeValue = (String) claims.get("typ");
            Number expiresAt = (Number) claims.get("exp");

            if (subject == null || tokenTypeValue == null || expiresAt == null) {
                return Optional.empty();
            }

            JwtTokenType tokenType = JwtTokenType.valueOf(tokenTypeValue);
            if (tokenType != expectedTokenType || Instant.now().getEpochSecond() >= expiresAt.longValue()) {
                return Optional.empty();
            }

            List<String> roles = objectMapper.convertValue(claims.get("roles"), new TypeReference<>() {
            });

            return Optional.of(new JwtClaims(subject, tokenType, roles == null ? List.of() : roles));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }

    private String createToken(User user, JwtTokenType tokenType, long expiresInSeconds) {
        Instant now = Instant.now();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", JWT_ALGORITHM);
        header.put("typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.getEmail());
        claims.put("typ", tokenType.name());
        claims.put("roles", roleNames(user));
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plusSeconds(expiresInSeconds).getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedClaims = encodeJson(claims);
        String signature = sign(encodedHeader + "." + encodedClaims);

        return encodedHeader + "." + encodedClaims + "." + signature;
    }

    private List<String> roleNames(User user) {
        if (user.getId() == null) {
            return List.of();
        }

        return userRoleRepository.findByIdUserId(user.getId())
                .stream()
                .map(userRole -> userRole.getRole().getName().name())
                .toList();
    }

    private boolean isSignatureValid(String encodedHeader, String encodedClaims, String encodedSignature) {
        String expectedSignature = sign(encodedHeader + "." + encodedClaims);
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                encodedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return base64UrlEncode(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create JWT", exception);
        }
    }

    private Map<String, Object> readJson(String encodedValue) {
        try {
            return objectMapper.readValue(base64UrlDecode(encodedValue), new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid JWT payload", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKey);
            return base64UrlEncode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
