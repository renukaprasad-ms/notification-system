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

    @EntityGraph(attributePaths = {"notification", "user"})
    Optional<NotificationRecipient> findDetailedById(UUID id);

    Optional<NotificationRecipient> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndReadAtIsNull(UUID userId);

    long countByNotificationId(UUID notificationId);

    @Query(
            """
            select nr.id
            from NotificationRecipient nr
            where nr.notification.id = :notificationId
            """
    )
    List<UUID> findIdsByNotificationId(@Param("notificationId") UUID notificationId);

    @Query(
            """
            select distinct nr.user.id
            from NotificationRecipient nr
            where nr.notification.id = :notificationId
              and nr.readAt is null
            """
    )
    List<UUID> findUnreadUserIdsByNotificationId(@Param("notificationId") UUID notificationId);

    @Modifying
    @Query(
            """
            delete from NotificationRecipient nr
            where nr.notification.id = :notificationId
            """
    )
    int deleteAllByNotificationId(@Param("notificationId") UUID notificationId);

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

    @EntityGraph(attributePaths = {"notification", "user"})
    @Query(
            value = """
                    select nr
                    from NotificationRecipient nr
                    join nr.notification n
                    where nr.user.id = :userId
                      and (
                            :search = '__all__'
                            or lower(coalesce(n.title, '')) like lower(concat('%', :search, '%'))
                            or lower(coalesce(n.message, '')) like lower(concat('%', :search, '%'))
                            or lower(str(n.type)) like lower(concat('%', :search, '%'))
                            or lower(str(n.priority)) like lower(concat('%', :search, '%'))
                      )
                    """,
            countQuery = """
                    select count(nr)
                    from NotificationRecipient nr
                    join nr.notification n
                    where nr.user.id = :userId
                      and (
                            :search = '__all__'
                            or lower(coalesce(n.title, '')) like lower(concat('%', :search, '%'))
                            or lower(coalesce(n.message, '')) like lower(concat('%', :search, '%'))
                            or lower(str(n.type)) like lower(concat('%', :search, '%'))
                            or lower(str(n.priority)) like lower(concat('%', :search, '%'))
                      )
                    """
    )
    Page<NotificationRecipient> findUserNotifications(
            @Param("userId") UUID userId,
            @Param("search") String search,
            Pageable pageable
    );
}
