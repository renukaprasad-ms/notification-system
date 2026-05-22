package com.renuka.notification_backend.auth.dto;

import java.util.List;
import java.util.UUID;

public class LoginResponse {

    private final UUID userId;
    private final String email;
    private final String fullName;
    private final List<String> roles;

    public LoginResponse(UUID userId, String email, String fullName, List<String> roles) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
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

    public List<String> getRoles() {
        return roles;
    }
}
