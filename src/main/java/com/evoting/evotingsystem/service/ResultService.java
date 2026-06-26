package com.evoting.evotingsystem.service;

import com.evoting.evotingsystem.dto.result.ElectionResultCardDto;
import com.evoting.evotingsystem.dto.result.RecentResultRowDto;
import com.evoting.evotingsystem.dto.result.ResultCandidateBarDto;
import com.evoting.evotingsystem.dto.result.ResultsPageDto;
import com.evoting.evotingsystem.entity.Election;
import com.evoting.evotingsystem.entity.ElectionStatus;
import com.evoting.evotingsystem.entity.User;
import com.evoting.evotingsystem.entity.UserRole;
import com.evoting.evotingsystem.repository.CandidateRepository;
import com.evoting.evotingsystem.repository.ElectionRepository;
import com.evoting.evotingsystem.repository.ResultRepository;
import com.evoting.evotingsystem.repository.UserNotificationRepository;
import com.evoting.evotingsystem.repository.UserRepository;
import com.evoting.evotingsystem.repository.VoteRepository;
import com.evoting.evotingsystem.repository.projection.CandidateVoteAggregateView;
import com.evoting.evotingsystem.repository.projection.CompletedElectionResultView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResultService {
    private static final Set<UserRole> STUDENT_ROLES = Set.of(UserRole.STUDENT, UserRole.VOTER);

    private final ResultRepository resultRepository;
    private final ElectionRepository electionRepository;
    private final VoteRepository voteRepository;
    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;
    private final UserNotificationRepository userNotificationRepository;

    @Transactional(readOnly = true)
    public ResultsPageDto getResultsPage(Long userId, int page, int size) {
        User user = userRepository.findByIdAndRoleIn(userId, STUDENT_ROLES)
                .orElseThrow(() -> new IllegalArgumentException("Student account not found."));

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);

        Page<CompletedElectionResultView> completedPage = resultRepository.findCompletedElectionResults(
                ElectionStatus.COMPLETED,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "endsAt"))
        );

        List<Long> electionIds = completedPage.getContent().stream()
                .map(CompletedElectionResultView::getElectionId)
                .toList();

        Map<Long, List<CandidateVoteAggregateView>> candidateMap = electionIds.isEmpty()
                ? Collections.emptyMap()
                : candidateRepository.findCandidateVotesForElectionIds(electionIds)
                        .stream()
                        .collect(Collectors.groupingBy(CandidateVoteAggregateView::getElectionId));

        long totalElections = electionRepository.count();
        long totalVotes = voteRepository.count();
        long activeElections = electionRepository.countByStatusIn(List.of(ElectionStatus.UPCOMING, ElectionStatus.ACTIVE));
        long totalVoters = userRepository.countByRoleIn(STUDENT_ROLES);
        double turnout = toPercentage(totalVotes, totalVoters);
        long unreadNotifications = userNotificationRepository.countByUserIdAndReadFlagFalse(userId);

        List<ElectionResultCardDto> cards = completedPage.getContent().stream()
                .map(row -> toCardDto(row, candidateMap.getOrDefault(row.getElectionId(), Collections.emptyList()), totalVoters))
                .toList();

        List<RecentResultRowDto> recentRows = completedPage.getContent().stream()
                .map(row -> toRecentRow(row, candidateMap.getOrDefault(row.getElectionId(), Collections.emptyList()), totalVoters))
                .toList();

        List<ResultCandidateBarDto> topCandidates = candidateRepository.findTopCandidatesByStatuses(
                        List.of(ElectionStatus.COMPLETED, ElectionStatus.ACTIVE),
                        PageRequest.of(0, 6)
                )
                .stream()
                .map(aggregate -> new ResultCandidateBarDto(
                        aggregate.getCandidateId(),
                        aggregate.getFullName(),
                        aggregate.getPartyName(),
                        aggregate.getImagePath(),
                        aggregate.getVoteCount(),
                        toPercentage(aggregate.getVoteCount(), totalVotes)
                ))
                .toList();

        return new ResultsPageDto(
                user.getFullName(),
                user.getProfileImagePath(),
                user.getUsername(),
                user.getRole().name(),
                unreadNotifications,
                totalElections,
                totalVotes,
                activeElections,
                turnout,
                cards,
                topCandidates,
                recentRows,
                completedPage.getNumber(),
                completedPage.getTotalPages(),
                completedPage.hasPrevious(),
                completedPage.hasNext(),
                Math.max(0, completedPage.getNumber() - 1),
                completedPage.getNumber() + 1
        );
    }

    @Transactional(readOnly = true)
    public String exportElectionResultCsv(Long electionId) {
        Election election = resultRepository.findById(electionId)
                .orElseThrow(() -> new IllegalArgumentException("Election not found."));
        List<CandidateVoteAggregateView> candidates = candidateRepository.findCandidateVotesForElectionIds(List.of(electionId));
        long totalVotes = candidates.stream().mapToLong(CandidateVoteAggregateView::getVoteCount).sum();
        String winner = candidates.stream().findFirst().map(CandidateVoteAggregateView::getFullName).orElse("No winner");

        StringBuilder csv = new StringBuilder();
        csv.append("election_id,election_title,status,winner,total_votes\n");
        csv.append(election.getId()).append(",")
                .append(escapeCsv(election.getTitle())).append(",")
                .append(election.getStatus()).append(",")
                .append(escapeCsv(winner)).append(",")
                .append(totalVotes).append("\n");
        csv.append("\n");
        csv.append("candidate,party,votes,percentage\n");
        for (CandidateVoteAggregateView candidate : candidates) {
            csv.append(escapeCsv(candidate.getFullName())).append(",")
                    .append(escapeCsv(candidate.getPartyName())).append(",")
                    .append(candidate.getVoteCount()).append(",")
                    .append(toPercentage(candidate.getVoteCount(), Math.max(1, totalVotes)))
                    .append("\n");
        }
        return csv.toString();
    }

    private ElectionResultCardDto toCardDto(
            CompletedElectionResultView row,
            List<CandidateVoteAggregateView> candidateRows,
            long totalVoters
    ) {
        CandidateVoteAggregateView winner = candidateRows.stream().findFirst().orElse(null);
        String winnerName = winner == null ? "No winner declared" : winner.getFullName();
        String winnerParty = winner == null ? "-" : winner.getPartyName();
        long winnerVotes = winner == null ? 0L : winner.getVoteCount();
        List<ResultCandidateBarDto> bars = candidateRows.stream()
                .limit(5)
                .map(candidate -> new ResultCandidateBarDto(
                        candidate.getCandidateId(),
                        candidate.getFullName(),
                        candidate.getPartyName(),
                        candidate.getImagePath(),
                        candidate.getVoteCount(),
                        toPercentage(candidate.getVoteCount(), Math.max(1, row.getTotalVotes()))
                ))
                .toList();

        return new ElectionResultCardDto(
                row.getElectionId(),
                row.getElectionTitle(),
                row.getStartsAt(),
                row.getEndsAt(),
                winnerName,
                winnerParty,
                winnerVotes,
                row.getTotalVotes(),
                toPercentage(row.getTotalVotes(), totalVoters),
                "COMPLETED",
                bars
        );
    }

    private RecentResultRowDto toRecentRow(
            CompletedElectionResultView row,
            List<CandidateVoteAggregateView> candidateRows,
            long totalVoters
    ) {
        String winnerName = candidateRows.stream()
                .findFirst()
                .map(CandidateVoteAggregateView::getFullName)
                .orElse("No winner declared");
        return new RecentResultRowDto(
                row.getElectionId(),
                row.getElectionTitle(),
                winnerName,
                row.getTotalVotes(),
                "PUBLISHED",
                row.getPublishedAt(),
                toPercentage(row.getTotalVotes(), totalVoters)
        );
    }

    private double toPercentage(long value, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(((value * 100.0) / total) * 100.0) / 100.0;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
