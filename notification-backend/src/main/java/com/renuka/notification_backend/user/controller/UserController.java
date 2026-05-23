package com.renuka.notification_backend.user.controller;

import com.renuka.notification_backend.common.response.ApiResponse;
import com.renuka.notification_backend.common.response.PageResponse;
import com.renuka.notification_backend.user.dto.AdminUserResponse;
import com.renuka.notification_backend.user.dto.UserProfileResponse;
import com.renuka.notification_backend.user.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile(Authentication authentication) {
        UserProfileResponse response = userService.getCurrentUserProfile(authentication.getName());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), response, "Profile fetched successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        PageResponse<AdminUserResponse> response = userService.getAllUsersForAdmin(page, size, search);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), response, "Users fetched successfully"));
    }
}
