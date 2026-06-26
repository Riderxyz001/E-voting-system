package com.evoting.evotingsystem.dto.election;

import java.util.List;

public record ActiveElectionsPageDto(
        String fullName,
        String profileImagePath,
        String username,
        String role,
        long unreadNotifications,
        String search,
        String statusFilter,
        List<ActiveElectionCardDto> elections,
        int pageNumber,
        int totalPages,
        long totalElements,
        boolean hasPrevious,
        boolean hasNext,
        int previousPage,
        int nextPage
) {
}
