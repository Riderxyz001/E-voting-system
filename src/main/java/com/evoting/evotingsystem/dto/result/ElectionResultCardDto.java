package com.evoting.evotingsystem.dto.result;

import java.time.LocalDateTime;
import java.util.List;

public record ElectionResultCardDto(
        Long electionId,
        String electionTitle,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String winnerName,
        String winnerParty,
        long winnerVotes,
        long totalVotes,
        double turnoutPercentage,
        String status,
        List<ResultCandidateBarDto> candidateBars
) {
}
