package com.renuka.notification_backend.notification.repository;

import com.renuka.notification_backend.notification.entity.NotificationRecipient;
import com.renuka.notification_backend.notification.entity.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, UUID> {

    @EntityGraph(attributePaths = {"notification", "user"})
    Page<NotificationRecipient> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"notification", "user"})
    Page<NotificationRecipient> findByNotificationId(UUID notificationId, Pageable pageable);

    Optional<NotificationRecipient> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndReadAtIsNull(UUID userId);

    long countByNotificationId(UUID notificationId);

    @Modifying
    @Query(
            value = """
                    insert into notification_recipients (id, notification_id, user_id, delivery_status, created_at)
                    select gen_random_uuid(), :notificationId, u.id, 'PENDING', now()
                    from users u
                    where u.is_active = true
                    """,
            nativeQuery = true
    )
    int insertPendingRecipientsForActiveUsers(@Param("notificationId") UUID notificationId);

    List<NotificationRecipient> findByDeliveryStatusInOrderByCreatedAtAsc(List<DeliveryStatus> deliveryStatuses, Pageable pageable);

    @EntityGraph(attributePaths = "notification")
    @Query(
            value = """
                    select nr.*
                    from notification_recipients nr
                    join notifications n on n.id = nr.notification_id
                    where nr.user_id = :userId
                      and (
                            :search = '__all__'
                            or n.title ilike concat('%', :search, '%')
                            or n.message ilike concat('%', :search, '%')
                            or n.type ilike concat('%', :search, '%')
                            or n.priority ilike concat('%', :search, '%')
                      )
                    order by nr.created_at desc
                    """,
            countQuery = """
                    select count(*)
                    from notification_recipients nr
                    join notifications n on n.id = nr.notification_id
                    where nr.user_id = :userId
                      and (
                            :search = '__all__'
                            or n.title ilike concat('%', :search, '%')
                            or n.message ilike concat('%', :search, '%')
                            or n.type ilike concat('%', :search, '%')
                            or n.priority ilike concat('%', :search, '%')
                      )
                    """,
            nativeQuery = true
    )
    Page<NotificationRecipient> findUserNotifications(
            @Param("userId") UUID userId,
            @Param("search") String search,
            Pageable pageable
    );
}
