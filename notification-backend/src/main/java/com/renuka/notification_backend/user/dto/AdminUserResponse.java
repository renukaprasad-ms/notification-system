package com.renuka.notification_backend.user.dto;

import java.util.List;
import java.util.UUID;

public class AdminUserResponse {

    private final UUID userId;
    private final String email;
    private final String fullName;
    private final boolean emailVerified;
    private final boolean active;
    private final List<String> roles;

    public AdminUserResponse(
            UUID userId,
            String email,
            String fullName,
            boolean emailVerified,
            boolean active,
            List<String> roles
    ) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.emailVerified = emailVerified;
        this.active = active;
        this.roles = roles;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean isActive() {
        return active;
    }

    public List<String> getRoles() {
        return roles;
    }
}
