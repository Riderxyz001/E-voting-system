package com.evoting.evotingsystem.dto.dashboard;

import java.util.List;

public record HomeDashboardDto(
        DashboardSummaryDto summary,
        List<ActiveElectionDto> activeElections,
        List<TopCandidateDto> topCandidates,
        List<RecentElectionResultDto> recentResults,
        List<FaqDto> faqs,
        List<TestimonialDto> testimonials
) {
}
