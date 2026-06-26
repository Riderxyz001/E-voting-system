package com.evoting.evotingsystem.service;

import com.evoting.evotingsystem.dto.notification.NotificationItemDto;
import com.evoting.evotingsystem.dto.notification.NotificationTabDto;
import com.evoting.evotingsystem.dto.notification.NotificationsPageDto;
import com.evoting.evotingsystem.entity.NotificationType;
import com.evoting.evotingsystem.entity.User;
import com.evoting.evotingsystem.entity.UserNotification;
import com.evoting.evotingsystem.entity.UserRole;
import com.evoting.evotingsystem.repository.UserNotificationRepository;
import com.evoting.evotingsystem.repository.UserRepository;
import com.evoting.evotingsystem.repository.projection.NotificationTypeCountView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Set<UserRole> STUDENT_ROLES = Set.of(UserRole.STUDENT, UserRole.VOTER);

    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public NotificationsPageDto getNotificationsPage(Long userId, String typeFilter, int page, int size) {
        User user = getVoterById(userId);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        NotificationType selectedType = resolveType(typeFilter);
        String selectedTypeKey = selectedType == null ? "ALL" : selectedType.name();

        Page<UserNotification> notificationsPage = selectedType == null
                ? userNotificationRepository.findByUserIdOrderByCreatedAtDesc(
                        userId,
                        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                : userNotificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                        userId,
                        selectedType,
                        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
                );

        long total = userNotificationRepository.countByUserId(userId);
        long unread = userNotificationRepository.countByUserIdAndReadFlagFalse(userId);
        long read = userNotificationRepository.countByUserIdAndReadFlagTrue(userId);
        long important = userNotificationRepository.countByUserIdAndTypeAndReadFlagFalse(userId, NotificationType.SECURITY)
                + userNotificationRepository.countByUserIdAndTypeAndReadFlagFalse(userId, NotificationType.RESULT);

        Map<NotificationType, Long> typeCounts = userNotificationRepository.countByTypeForUser(userId)
                .stream()
                .collect(Collectors.toMap(NotificationTypeCountView::getType, NotificationTypeCountView::getTotal));

        List<NotificationTabDto> tabs = buildTabs(typeCounts, selectedTypeKey, total);
        List<NotificationItemDto> items = notificationsPage.getContent().stream().map(this::toItemDto).toList();
        List<NotificationItemDto> recentItems = userNotificationRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toItemDto)
                .toList();

        return new NotificationsPageDto(
                user.getFullName(),
                user.getProfileImagePath(),
                user.getUsername(),
                user.getRole().name(),
                unread,
                total,
                read,
                unread,
                important,
                selectedTypeKey,
                tabs,
                items,
                recentItems,
                notificationsPage.getNumber(),
                notificationsPage.getTotalPages(),
                notificationsPage.hasPrevious(),
                notificationsPage.hasNext(),
                Math.max(0, notificationsPage.getNumber() - 1),
                notificationsPage.getNumber() + 1
        );
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        getVoterById(userId);
        userNotificationRepository.markAllAsReadByUser(userId);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        getVoterById(userId);
        userNotificationRepository.markAsRead(userId, notificationId);
    }

    @Transactional
    public void enableNotifications(Long userId) {
        User user = getVoterById(userId);
        user.setLoginNotificationsEnabled(true);
        userRepository.save(user);
    }

    private User getVoterById(Long userId) {
        return userRepository.findByIdAndRoleIn(userId, STUDENT_ROLES)
                .orElseThrow(() -> new IllegalArgumentException("Student account not found."));
    }

    private List<NotificationTabDto> buildTabs(Map<NotificationType, Long> typeCounts, String selectedType, long total) {
        Map<NotificationType, String> labels = Map.of(
                NotificationType.ELECTION, "Elections",
                NotificationType.VOTE, "Votes",
                NotificationType.RESULT, "Results",
                NotificationType.ANNOUNCEMENT, "Announcements",
                NotificationType.SECURITY, "Security"
        );

        List<NotificationTabDto> tabs = Arrays.stream(NotificationType.values())
                .map(type -> new NotificationTabDto(
                        type.name(),
                        labels.get(type),
                        typeCounts.getOrDefault(type, 0L),
                        type.name().equals(selectedType)
                ))
                .collect(Collectors.toList());

        tabs.add(0, new NotificationTabDto("ALL", "All", total, "ALL".equals(selectedType)));
        return tabs;
    }

    private NotificationItemDto toItemDto(UserNotification notification) {
        NotificationType type = notification.getType() == null ? NotificationType.ANNOUNCEMENT : notification.getType();
        Map<NotificationType, String> iconMap = Map.of(
                NotificationType.ELECTION, "bi-megaphone-fill",
                NotificationType.VOTE, "bi-check2-square",
                NotificationType.RESULT, "bi-bar-chart-fill",
                NotificationType.ANNOUNCEMENT, "bi-info-circle-fill",
                NotificationType.SECURITY, "bi-shield-lock-fill"
        );
        Map<NotificationType, String> iconBgMap = Map.of(
                NotificationType.ELECTION, "notification-icon-election",
                NotificationType.VOTE, "notification-icon-vote",
                NotificationType.RESULT, "notification-icon-result",
                NotificationType.ANNOUNCEMENT, "notification-icon-announcement",
                NotificationType.SECURITY, "notification-icon-security"
        );
        Map<NotificationType, String> badgeMap = Map.of(
                NotificationType.ELECTION, "text-bg-primary-subtle text-primary-emphasis",
                NotificationType.VOTE, "text-bg-success-subtle text-success-emphasis",
                NotificationType.RESULT, "text-bg-warning-subtle text-warning-emphasis",
                NotificationType.ANNOUNCEMENT, "text-bg-info-subtle text-info-emphasis",
                NotificationType.SECURITY, "text-bg-danger-subtle text-danger-emphasis"
        );

        return new NotificationItemDto(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                type.name(),
                iconMap.get(type),
                iconBgMap.get(type),
                badgeMap.get(type),
                Boolean.TRUE.equals(notification.getReadFlag()),
                notification.getCreatedAt(),
                notification.getRedirectUrl()
        );
    }

    private NotificationType resolveType(String rawFilter) {
        if (rawFilter == null || rawFilter.isBlank() || "ALL".equalsIgnoreCase(rawFilter)) {
            return null;
        }
        try {
            return NotificationType.valueOf(rawFilter.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
