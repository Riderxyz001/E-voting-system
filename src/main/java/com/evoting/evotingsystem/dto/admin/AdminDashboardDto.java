package com.evoting.evotingsystem.dto.admin;

import java.util.List;

public record AdminDashboardDto(
        String username,
        String role,
        long unreadNotifications,
        long totalVoters,
        long activeElections,
        long totalVotesCast,
        long totalCandidates,
        List<AdminActivityDto> recentActivities,
        long completedElections,
        long upcomingElections,
        long ongoingElections
) {
}
