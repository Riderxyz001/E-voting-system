package com.evoting.evotingsystem.dto.dashboard;

public record DashboardSummaryDto(
        long totalRegisteredVoters,
        long totalActiveElections,
        long totalCandidates,
        long totalVotesCast,
        double systemUptimePercentage
) {
}
