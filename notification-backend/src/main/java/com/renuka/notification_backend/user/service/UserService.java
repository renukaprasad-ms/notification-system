package com.renuka.notification_backend.user.service;

import com.renuka.notification_backend.common.exception.UnauthorizedException;
import com.renuka.notification_backend.common.response.PageResponse;
import com.renuka.notification_backend.user.dto.AdminUserResponse;
import com.renuka.notification_backend.user.dto.UserProfileResponse;
import com.renuka.notification_backend.user.entity.User;
import com.renuka.notification_backend.user.repository.UserRepository;
import com.renuka.notification_backend.user.repository.UserRoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private static final String MATCH_ALL_SEARCH = "__all__";

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

@Transactional
public PageResponse<AdminUserResponse> getAllUsersForAdmin(int page, int size, String search) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);

    Pageable pageable = PageRequest.of(
            safePage,
            safeSize,
            Sort.by(Sort.Direction.DESC, "createdAt")
    );

    String normalizedSearch = normalizeSearch(search);

    Page<User> usersPage;

    if (MATCH_ALL_SEARCH.equals(normalizedSearch)) {
        usersPage = userRepository.findAll(pageable);
    } else {
        usersPage = userRepository.searchUsers(normalizedSearch, pageable);
    }

    Page<AdminUserResponse> responsePage = usersPage.map(user -> new AdminUserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.isEmailVerified(),
            user.isActive(),
            getRoles(user.getId())
    ));

    return PageResponse.from(responsePage);
}

    private List<String> getRoles(java.util.UUID userId) {
        return userRoleRepository.findByIdUserId(userId)
                .stream()
                .map(userRole -> userRole.getRole().getName().name())
                .toList();
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return MATCH_ALL_SEARCH;
        }
        return search.trim();
    }

    private boolean matchesSearch(AdminUserResponse user, String search) {
        if (MATCH_ALL_SEARCH.equals(search) || search.isBlank()) {
            return true;
        }

        String needle = search.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(user.getFullName(), needle)
                || containsIgnoreCase(user.getEmail(), needle)
                || user.getRoles().stream().anyMatch(role -> role.toLowerCase(Locale.ROOT).contains(needle));
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
