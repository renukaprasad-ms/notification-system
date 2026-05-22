package com.renuka.notification_backend.user.repository;

import com.renuka.notification_backend.user.entity.UserRole;
import com.renuka.notification_backend.user.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}
