package com.evoting.evotingsystem.dto.notification;

public record NotificationTabDto(
        String key,
        String label,
        long count,
        boolean active
) {
}
