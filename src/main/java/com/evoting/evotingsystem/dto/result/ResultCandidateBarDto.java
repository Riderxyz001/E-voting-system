package com.evoting.evotingsystem.dto.result;

public record ResultCandidateBarDto(
        Long candidateId,
        String candidateName,
        String partyName,
        String imagePath,
        long voteCount,
        double votePercentage
) {
}
