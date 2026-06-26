package com.evoting.evotingsystem.repository;

import com.evoting.evotingsystem.entity.UserNotification;
import com.evoting.evotingsystem.entity.NotificationType;
import com.evoting.evotingsystem.repository.projection.NotificationTypeCountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findTop8ByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserNotification> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFlagFalse(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndReadFlagTrue(Long userId);

    long countByUserIdAndTypeAndReadFlagFalse(Long userId, NotificationType type);

    long countByUserIdAndType(Long userId, NotificationType type);

    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<UserNotification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type, Pageable pageable);

    @Query("""
            select
                un.type as type,
                count(un.id) as total
            from UserNotification un
            where un.user.id = :userId
            group by un.type
            """)
    List<NotificationTypeCountView> countByTypeForUser(@Param("userId") Long userId);

    @Modifying
    @Query("""
            update UserNotification un
            set un.readFlag = true
            where un.user.id = :userId
              and un.readFlag = false
            """)
    int markAllAsReadByUser(@Param("userId") Long userId);

    @Modifying
    @Query("""
            update UserNotification un
            set un.readFlag = true
            where un.user.id = :userId
              and un.id = :notificationId
            """)
    int markAsRead(@Param("userId") Long userId, @Param("notificationId") Long notificationId);
}
