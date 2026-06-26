package com.evoting.evotingsystem.dto.vote;

import java.util.List;

public record MyVotesPageDto(
        String fullName,
        String profileImagePath,
        String username,
        String role,
        long unreadNotifications,
        long electionsVoted,
        long votesCast,
        long upcomingVotes,
        double participationPercentage,
        List<MyVoteHistoryRowDto> voteHistory,
        int pageNumber,
        int totalPages,
        long totalElements,
        boolean hasPrevious,
        boolean hasNext,
        int previousPage,
        int nextPage
) {
}
