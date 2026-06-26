package com.evoting.evotingsystem.dto.result;

import java.time.LocalDateTime;

public record RecentResultRowDto(
        Long electionId,
        String electionTitle,
        String winnerName,
        long totalVotes,
        String resultStatus,
        LocalDateTime publishedDate,
        double turnoutPercentage
) {
}
