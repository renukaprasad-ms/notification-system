package com.renuka.notification_backend.user.service;

import com.renuka.notification_backend.common.exception.UnauthorizedException;
import com.renuka.notification_backend.user.dto.UserProfileResponse;
import com.renuka.notification_backend.user.entity.User;
import com.renuka.notification_backend.user.repository.UserRepository;
import com.renuka.notification_backend.user.repository.UserRoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public UserService(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Transactional
    public UserProfileResponse getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authentication is required"));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        List<String> roles = userRoleRepository.findByIdUserId(user.getId())
                .stream()
                .map(userRole -> userRole.getRole().getName().name())
                .toList();

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.isEmailVerified(),
                roles
        );
    }
}
