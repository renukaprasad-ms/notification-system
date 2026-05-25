package com.renuka.notification_backend;

import com.renuka.notification_backend.auth.service.PasswordHashService;
import com.renuka.notification_backend.security.jwt.JwtService;
import com.renuka.notification_backend.user.entity.Role;
import com.renuka.notification_backend.user.entity.RoleName;
import com.renuka.notification_backend.user.entity.User;
import com.renuka.notification_backend.user.entity.UserRole;
import com.renuka.notification_backend.user.repository.RoleRepository;
import com.renuka.notification_backend.user.repository.UserRepository;
import com.renuka.notification_backend.user.repository.UserRoleRepository;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestSupport {

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected UserRoleRepository userRoleRepository;

    @Autowired
    protected PasswordHashService passwordHashService;

    @Autowired
    protected JwtService jwtService;

    protected String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize test payload", exception);
        }
    }

    protected MediaType json() {
        return MediaType.APPLICATION_JSON;
    }

    protected User createUserWithRoles(String email, String password, String fullName, RoleName... roles) {
        User user = new User();
        user.setEmail(email.trim().toLowerCase());
        user.setFullName(fullName);
        user.setPasswordHash(passwordHashService.hashPassword(password));
        user.setEmailVerified(true);
        user.setActive(true);
        User savedUser = userRepository.save(user);

        for (RoleName roleName : roles) {
            Role role = roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setName(roleName);
                        newRole.setDescription(roleName + " role");
                        return roleRepository.save(newRole);
                    });
            userRoleRepository.save(new UserRole(savedUser, role));
        }

        return savedUser;
    }

    protected MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder requestBuilder, User user) {
        return requestBuilder.cookie(new Cookie("access_token", jwtService.createAccessToken(user)));
    }

    protected String uniqueEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID() + "@example.com";
    }
}
