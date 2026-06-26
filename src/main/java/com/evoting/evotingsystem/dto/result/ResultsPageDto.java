package com.evoting.evotingsystem.dto.result;

import java.util.List;

public record ResultsPageDto(
        String fullName,
        String profileImagePath,
        String username,
        String role,
        long unreadNotifications,
        long totalElections,
        long totalVotes,
        long activeElections,
        double voterTurnoutPercentage,
        List<ElectionResultCardDto> electionResults,
        List<ResultCandidateBarDto> topCandidates,
        List<RecentResultRowDto> recentResults,
        int pageNumber,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext,
        int previousPage,
        int nextPage
) {
}
