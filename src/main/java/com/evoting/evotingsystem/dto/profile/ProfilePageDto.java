package com.evoting.evotingsystem.dto.profile;

import java.time.LocalDateTime;

public record ProfilePageDto(
        Long userId,
        String username,
        String fullName,
        String profileImagePath,
        String role,
        String email,
        String phoneNumber,
        LocalDateTime joinedAt,
        long unreadNotifications,
        long electionsVoted,
        long votesCast,
        double participationPercentage,
        String accountStatus,
        String accountCode,
        LocalDateTime lastLoginAt,
        boolean twoFactorEnabled,
        boolean loginNotificationsEnabled
) {
}
