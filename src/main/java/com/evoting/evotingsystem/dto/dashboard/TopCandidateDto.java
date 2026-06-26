package com.evoting.evotingsystem.dto.dashboard;

public record TopCandidateDto(
        Long candidateId,
        String candidateName,
        String partyName,
        String imagePath,
        String electionTitle,
        long voteCount,
        double votePercentage
) {
}
