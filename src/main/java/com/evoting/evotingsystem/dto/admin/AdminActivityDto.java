package com.evoting.evotingsystem.dto.admin;

import java.time.LocalDateTime;

public record AdminActivityDto(
        String title,
        String message,
        String iconClass,
        LocalDateTime createdAt
) {
}
