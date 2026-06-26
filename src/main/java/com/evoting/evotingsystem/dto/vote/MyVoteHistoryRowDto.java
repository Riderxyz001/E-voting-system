package com.evoting.evotingsystem.dto.vote;

import java.time.LocalDateTime;

public record MyVoteHistoryRowDto(
        Long electionId,
        String electionTitle,
        LocalDateTime electionStartsAt,
        LocalDateTime electionEndsAt,
        long totalCandidates,
        Long selectedCandidateId,
        String selectedCandidateName,
        String selectedCandidateParty,
        LocalDateTime votedAt,
        String status,
        String statusBadgeClass
) {
}
