package com.evoting.evotingsystem.dto.dashboard;

import java.time.LocalDateTime;

public record ActiveElectionDto(
        Long id,
        String title,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        long totalCandidates,
        long totalVotes,
        String status,
        boolean alreadyVoted
) {
}
