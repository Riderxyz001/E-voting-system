package com.evoting.evotingsystem.dto.dashboard;

import java.util.List;

public record RecentElectionResultDto(
        Long electionId,
        String electionTitle,
        String winnerName,
        String winnerParty,
        long totalVotes,
        double winnerPercentage,
        List<CandidateResultDto> candidateResults
) {
}
