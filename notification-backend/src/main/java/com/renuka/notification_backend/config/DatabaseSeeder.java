package com.renuka.notification_backend.config;

import com.renuka.notification_backend.user.entity.Role;
import com.renuka.notification_backend.user.entity.RoleName;
import com.renuka.notification_backend.user.entity.User;
import com.renuka.notification_backend.user.entity.UserRole;
import com.renuka.notification_backend.user.entity.UserRoleId;
import com.renuka.notification_backend.user.repository.RoleRepository;
import com.renuka.notification_backend.user.repository.UserRepository;
import com.renuka.notification_backend.user.repository.UserRoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DatabaseSeeder implements ApplicationRunner {

    private final boolean seedEnabled;
    private final String superAdminEmail;
    private final String superAdminPassword;
    private final String superAdminFullName;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(
            @Value("${app.seed.enabled:true}") boolean seedEnabled,
            @Value("${app.seed.super-admin.email:admin@example.com}") String superAdminEmail,
            @Value("${app.seed.super-admin.password:Admin@12345}") String superAdminPassword,
            @Value("${app.seed.super-admin.full-name:Super Admin}") String superAdminFullName,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.seedEnabled = seedEnabled;
        this.superAdminEmail = superAdminEmail;
        this.superAdminPassword = superAdminPassword;
        this.superAdminFullName = superAdminFullName;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }

        validateSeedConfig();

        Role userRole = createRoleIfMissing(RoleName.USER, "Default application user");
        Role adminRole = createRoleIfMissing(RoleName.ADMIN, "Administrator with notification management access");

        User superAdmin = userRepository.findByEmail(superAdminEmail)
                .orElseGet(this::createSuperAdmin);

        assignRoleIfMissing(superAdmin, userRole);
        assignRoleIfMissing(superAdmin, adminRole);
    }

    private void validateSeedConfig() {
        if (!StringUtils.hasText(superAdminEmail)) {
            throw new IllegalStateException("Super admin seed email is required");
        }

        if (!StringUtils.hasText(superAdminPassword)) {
            throw new IllegalStateException("Super admin seed password is required");
        }
    }

    private Role createRoleIfMissing(RoleName roleName, String description) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(roleName);
                    role.setDescription(description);
                    return roleRepository.save(role);
                });
    }

    private User createSuperAdmin() {
        User user = new User();
        user.setEmail(superAdminEmail);
        user.setFullName(superAdminFullName);
        user.setPasswordHash(passwordEncoder.encode(superAdminPassword));
        user.setEmailVerified(true);
        user.setActive(true);
        return userRepository.save(user);
    }

    private void assignRoleIfMissing(User user, Role role) {
        UserRoleId userRoleId = new UserRoleId(user.getId(), role.getId());
        if (userRoleRepository.existsById(userRoleId)) {
            return;
        }

        userRoleRepository.save(new UserRole(user, role));
    }
}
