package com.evoting.evotingsystem.dto.dashboard;

import java.util.List;

public record UserDashboardDto(
        Long userId,
        String fullName,
        String profileImagePath,
        String username,
        String role,
        UserDashboardSummaryDto summary,
        List<ActiveElectionDto> activeElections,
        UserVotingStatusDto votingStatus,
        List<TopCandidateDto> topCandidates,
        List<RecentElectionResultDto> recentResults,
        List<UserNotificationDto> notifications,
        long unreadNotifications
) {
}
