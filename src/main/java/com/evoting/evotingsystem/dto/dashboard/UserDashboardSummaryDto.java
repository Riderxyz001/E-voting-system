package com.evoting.evotingsystem.dto.dashboard;

public record UserDashboardSummaryDto(
        long totalActiveElections,
        long votesCastByUser,
        long totalCandidates,
        long totalVotesCast,
        double systemUptimePercentage
) {
}
