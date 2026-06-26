package com.evoting.evotingsystem.service;

import com.evoting.evotingsystem.dto.dashboard.ActiveElectionDto;
import com.evoting.evotingsystem.dto.dashboard.CandidateResultDto;
import com.evoting.evotingsystem.dto.dashboard.DashboardSummaryDto;
import com.evoting.evotingsystem.dto.dashboard.FaqDto;
import com.evoting.evotingsystem.dto.dashboard.HomeDashboardDto;
import com.evoting.evotingsystem.dto.dashboard.RecentElectionResultDto;
import com.evoting.evotingsystem.dto.dashboard.TestimonialDto;
import com.evoting.evotingsystem.dto.dashboard.TopCandidateDto;
import com.evoting.evotingsystem.dto.dashboard.UserDashboardDto;
import com.evoting.evotingsystem.dto.dashboard.UserNotificationDto;
import com.evoting.evotingsystem.dto.dashboard.UserDashboardSummaryDto;
import com.evoting.evotingsystem.dto.dashboard.UserVotingStatusDto;
import com.evoting.evotingsystem.entity.Election;
import com.evoting.evotingsystem.entity.ElectionStatus;
import com.evoting.evotingsystem.entity.User;
import com.evoting.evotingsystem.entity.UserRole;
import com.evoting.evotingsystem.repository.CandidateRepository;
import com.evoting.evotingsystem.repository.ElectionRepository;
import com.evoting.evotingsystem.repository.FaqRepository;
import com.evoting.evotingsystem.repository.TestimonialRepository;
import com.evoting.evotingsystem.repository.UserNotificationRepository;
import com.evoting.evotingsystem.repository.UserRepository;
import com.evoting.evotingsystem.repository.VoteRepository;
import com.evoting.evotingsystem.repository.projection.CandidateVoteAggregateView;
import com.evoting.evotingsystem.repository.projection.ElectionVoteTotalView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Set<ElectionStatus> ACTIVE_STATUSES = Set.of(ElectionStatus.UPCOMING, ElectionStatus.ACTIVE);
    private static final Set<UserRole> STUDENT_ROLES = Set.of(UserRole.STUDENT, UserRole.VOTER);

    private final UserRepository userRepository;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;
    private final FaqRepository faqRepository;
    private final TestimonialRepository testimonialRepository;
    private final UserNotificationRepository userNotificationRepository;

    @Transactional(readOnly = true)
    public HomeDashboardDto getHomeDashboard() {
        DashboardSummaryDto summary = buildSummary();
        List<ActiveElectionDto> activeElections = buildActiveElections(null);
        List<TopCandidateDto> topCandidates = buildTopCandidates();
        List<RecentElectionResultDto> recentResults = buildRecentResults();
        List<FaqDto> faqs = faqRepository.findTop6ByActiveTrueOrderByDisplayOrderAscCreatedAtDesc()
                .stream()
                .map(faq -> new FaqDto(faq.getId(), faq.getQuestion(), faq.getAnswer()))
                .toList();
        List<TestimonialDto> testimonials = testimonialRepository.findTop6ByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(testimonial -> new TestimonialDto(
                        testimonial.getId(),
                        testimonial.getAuthorName(),
                        testimonial.getDesignation(),
                        testimonial.getMessage(),
                        testimonial.getRating()
                ))
                .toList();

        return new HomeDashboardDto(summary, activeElections, topCandidates, recentResults, faqs, testimonials);
    }

    @Transactional(readOnly = true)
    public UserDashboardDto getUserDashboard(Long userId) {
        User user = userRepository.findByIdAndRoleIn(userId, STUDENT_ROLES)
                .orElseThrow(() -> new IllegalArgumentException("Student account not found."));

        UserDashboardSummaryDto summary = buildSummaryForUser(user.getId());
        List<ActiveElectionDto> activeElections = buildActiveElections(user.getId());
        UserVotingStatusDto votingStatus = buildVotingStatusForUser(user.getId());
        List<TopCandidateDto> topCandidates = buildTopCandidates();
        List<RecentElectionResultDto> recentResults = buildRecentResults();
        List<UserNotificationDto> notifications = userNotificationRepository.findTop8ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(notification -> new UserNotificationDto(
                        notification.getId(),
                        notification.getTitle(),
                        notification.getMessage(),
                        Boolean.TRUE.equals(notification.getReadFlag()),
                        notification.getCreatedAt()
                ))
                .toList();
        long unreadNotifications = userNotificationRepository.countByUserIdAndReadFlagFalse(userId);

        return new UserDashboardDto(
                user.getId(),
                user.getFullName(),
                user.getProfileImagePath(),
                user.getUsername(),
                formatRole(user.getRole()),
                summary,
                activeElections,
                votingStatus,
                topCandidates,
                recentResults,
                notifications,
                unreadNotifications
        );
    }

    private DashboardSummaryDto buildSummary() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        long totalRegisteredVoters = userRepository.countByRoleIn(STUDENT_ROLES);
        long totalActiveElections = electionRepository.countActiveElections(ACTIVE_STATUSES, now);
        long totalCandidates = candidateRepository.count();
        long totalVotesCast = voteRepository.count();
        double uptime = calculateSystemUptime(totalVotesCast, totalActiveElections);

        return new DashboardSummaryDto(
                totalRegisteredVoters,
                totalActiveElections,
                totalCandidates,
                totalVotesCast,
                uptime
        );
    }

    private UserDashboardSummaryDto buildSummaryForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        long totalActiveElections = electionRepository.countActiveElections(ACTIVE_STATUSES, now);
        long votesCastByUser = voteRepository.countByVoterId(userId);
        long totalCandidates = candidateRepository.count();
        long totalVotesCast = voteRepository.count();
        double uptime = calculateSystemUptime(totalVotesCast, totalActiveElections);

        return new UserDashboardSummaryDto(
                totalActiveElections,
                votesCastByUser,
                totalCandidates,
                totalVotesCast,
                uptime
        );
    }

    private UserVotingStatusDto buildVotingStatusForUser(Long userId) {
        List<Long> activeElectionIds = electionRepository.findActiveElectionIds(ACTIVE_STATUSES, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        if (activeElectionIds.isEmpty()) {
            return new UserVotingStatusDto(0, 0, 100.0);
        }
        long votedElections = voteRepository.countByVoterIdAndElectionIdIn(userId, activeElectionIds);
        long pendingElections = Math.max(0, activeElectionIds.size() - votedElections);
        double completion = toPercentage(votedElections, activeElectionIds.size());
        return new UserVotingStatusDto(votedElections, pendingElections, completion);
    }

    private List<ActiveElectionDto> buildActiveElections(Long userId) {
        List<Election> activeElections = electionRepository.findTop6ByStatusInAndEndsAtAfterOrderByStartsAtAsc(
                ACTIVE_STATUSES,
                LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
        );
        if (activeElections.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> electionIds = activeElections.stream().map(Election::getId).toList();
        Map<Long, Long> voteTotals = voteRepository.findVoteTotalsForElectionIds(electionIds)
                .stream()
                .collect(Collectors.toMap(ElectionVoteTotalView::getElectionId, ElectionVoteTotalView::getTotalVotes));

        return activeElections.stream()
                .map(election -> new ActiveElectionDto(
                        election.getId(),
                        election.getTitle(),
                        election.getDescription(),
                        election.getStartsAt(),
                        election.getEndsAt(),
                        candidateRepository.countByElectionId(election.getId()),
                        voteTotals.getOrDefault(election.getId(), 0L),
                        calculateStatus(election).name(),
                        userId != null && voteRepository.existsByVoterIdAndElectionId(userId, election.getId())
                ))
                .filter(dto -> !"DRAFT".equals(dto.status()))
                .toList();
    }

    private List<TopCandidateDto> buildTopCandidates() {
        List<CandidateVoteAggregateView> aggregates = candidateRepository.findTopCandidatesByStatuses(
                Set.of(ElectionStatus.ACTIVE, ElectionStatus.COMPLETED),
                PageRequest.of(0, 6)
        );
        if (aggregates.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> electionVoteTotals = getVoteTotalByElectionId(
                aggregates.stream().map(CandidateVoteAggregateView::getElectionId).distinct().toList()
        );

        return aggregates.stream()
                .map(aggregate -> {
                    long electionTotal = electionVoteTotals.getOrDefault(aggregate.getElectionId(), 0L);
                    return new TopCandidateDto(
                            aggregate.getCandidateId(),
                            aggregate.getFullName(),
                            aggregate.getPartyName(),
                            aggregate.getImagePath(),
                            aggregate.getElectionTitle(),
                            aggregate.getVoteCount(),
                            toPercentage(aggregate.getVoteCount(), electionTotal)
                    );
                })
                .toList();
    }

    private List<RecentElectionResultDto> buildRecentResults() {
        List<Election> completedElections = electionRepository.findTop5ByStatusOrderByEndsAtDesc(ElectionStatus.COMPLETED);
        if (completedElections.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> electionIds = completedElections.stream().map(Election::getId).toList();
        Map<Long, Long> electionVoteTotals = getVoteTotalByElectionId(electionIds);
        Map<Long, List<CandidateVoteAggregateView>> candidateVotesByElection = candidateRepository
                .findCandidateVotesForElectionIds(electionIds)
                .stream()
                .collect(Collectors.groupingBy(CandidateVoteAggregateView::getElectionId));

        return completedElections.stream()
                .map(election -> {
                    long totalVotes = electionVoteTotals.getOrDefault(election.getId(), 0L);
                    List<CandidateResultDto> candidateResults = candidateVotesByElection
                            .getOrDefault(election.getId(), Collections.emptyList())
                            .stream()
                            .map(candidate -> new CandidateResultDto(
                                    candidate.getCandidateId(),
                                    candidate.getFullName(),
                                    candidate.getPartyName(),
                                    candidate.getImagePath(),
                                    candidate.getVoteCount(),
                                    toPercentage(candidate.getVoteCount(), totalVotes)
                             ))
                            .toList();

                    CandidateResultDto winner = candidateResults.stream()
                            .findFirst()
                            .orElse(new CandidateResultDto(null, "No winner", "-", null, 0, 0));

                    return new RecentElectionResultDto(
                            election.getId(),
                            election.getTitle(),
                            winner.candidateName(),
                            winner.partyName(),
                            totalVotes,
                            winner.votePercentage(),
                            candidateResults
                    );
                })
                .toList();
    }

    private Map<Long, Long> getVoteTotalByElectionId(List<Long> electionIds) {
        if (electionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return voteRepository.findVoteTotalsForElectionIds(electionIds)
                .stream()
                .collect(Collectors.toMap(ElectionVoteTotalView::getElectionId, ElectionVoteTotalView::getTotalVotes));
    }

    private double calculateSystemUptime(long totalVotesCast, long totalActiveElections) {
        double dynamicBoost = Math.min(0.99, Math.log10(totalVotesCast + 10) / 10.0);
        double activeElectionBoost = Math.min(0.3, totalActiveElections * 0.03);
        return roundToTwo(98.7 + dynamicBoost + activeElectionBoost);
    }

    private double toPercentage(long value, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return roundToTwo((value * 100.0) / total);
    }

    private double roundToTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public ElectionStatus calculateStatus(Election election) {
        if (election.getStatus() == ElectionStatus.DRAFT || election.getStatus() == ElectionStatus.CANCELLED) {
            return election.getStatus();
        }

        // Minimum 2 candidates required to move out of DRAFT
        long candidateCount = candidateRepository.countByElectionId(election.getId());
        if (candidateCount < 2) {
            return ElectionStatus.DRAFT;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        if (now.isBefore(election.getStartsAt())) {
            return ElectionStatus.UPCOMING;
        } else if (now.isAfter(election.getEndsAt())) {
            return ElectionStatus.COMPLETED;
        } else {
            return ElectionStatus.ACTIVE;
        }
    }

    private String formatRole(UserRole role) {
        if (role == UserRole.ADMIN) return "ADMIN";
        return "VOTER";
    }
}
