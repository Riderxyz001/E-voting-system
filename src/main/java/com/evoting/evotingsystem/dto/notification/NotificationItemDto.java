package com.evoting.evotingsystem.dto.notification;

import java.time.LocalDateTime;

public record NotificationItemDto(
        Long id,
        String title,
        String message,
        String type,
        String iconClass,
        String iconBgClass,
        String badgeClass,
        boolean read,
        LocalDateTime createdAt,
        String redirectUrl
) {
}
