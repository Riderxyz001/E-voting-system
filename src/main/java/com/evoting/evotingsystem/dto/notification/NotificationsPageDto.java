package com.evoting.evotingsystem.dto.notification;

import java.util.List;

public record NotificationsPageDto(
        String fullName,
        String profileImagePath,
        String username,
        String role,
        long unreadNotifications,
        long totalNotifications,
        long readNotifications,
        long unreadCount,
        long importantNotifications,
        String selectedType,
        List<NotificationTabDto> tabs,
        List<NotificationItemDto> notifications,
        List<NotificationItemDto> recentNotifications,
        int pageNumber,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext,
        int previousPage,
        int nextPage
) {
}
