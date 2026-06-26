package com.evoting.evotingsystem.dto.election;

import java.time.LocalDateTime;

public record ActiveElectionCardDto(
        Long id,
        String title,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String status,
        String statusBadgeClass,
        long totalCandidates,
        long daysLeft
) {
}
