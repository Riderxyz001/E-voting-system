package com.evoting.evotingsystem.dto.dashboard;

public record CandidateResultDto(
        Long candidateId,
        String candidateName,
        String partyName,
        String imagePath,
        long voteCount,
        double votePercentage
) {
}
