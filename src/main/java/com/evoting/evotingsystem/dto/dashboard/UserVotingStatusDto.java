package com.evoting.evotingsystem.dto.dashboard;

public record UserVotingStatusDto(
        long votedElections,
        long pendingElections,
        double completionPercentage
) {
}
