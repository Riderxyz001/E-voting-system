package com.evoting.evotingsystem.dto.dashboard;

import java.time.LocalDateTime;

public record UserNotificationDto(
        Long id,
        String title,
        String message,
        boolean read,
        LocalDateTime createdAt
) {
}
