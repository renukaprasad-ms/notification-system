package com.renuka.notification_backend.user.repository;

import com.renuka.notification_backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByActiveTrue();

    long countByActiveTrue();

    List<User> findByIdInAndActiveTrue(List<UUID> ids);

    @Query("""
            select distinct u
            from User u
            where :search = '__all__'
                or lower(coalesce(u.fullName, '')) like lower(concat('%', :search, '%'))
                or lower(u.email) like lower(concat('%', :search, '%'))
                or exists (
                    select ur
                    from UserRole ur
                    where ur.user = u
                      and lower(str(ur.role.name)) like lower(concat('%', :search, '%'))
                )
            """)
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);
}
