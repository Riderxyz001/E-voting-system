package com.evoting.evotingsystem.service;

import com.evoting.evotingsystem.dto.vote.MyVoteHistoryRowDto;
import com.evoting.evotingsystem.dto.vote.MyVotesPageDto;
import com.evoting.evotingsystem.entity.ElectionStatus;
import com.evoting.evotingsystem.entity.User;
import com.evoting.evotingsystem.entity.UserRole;
import com.evoting.evotingsystem.entity.Candidate;
import com.evoting.evotingsystem.entity.Election;
import com.evoting.evotingsystem.entity.Vote;
import com.evoting.evotingsystem.repository.CandidateRepository;
import com.evoting.evotingsystem.repository.ElectionRepository;
import com.evoting.evotingsystem.repository.UserNotificationRepository;
import com.evoting.evotingsystem.repository.UserRepository;
import com.evoting.evotingsystem.repository.VoteRepository;
import com.evoting.evotingsystem.repository.projection.VoterElectionHistoryView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VoteService {

    private static final Set<ElectionStatus> UPCOMING_ELIGIBLE_STATUSES = Set.of(ElectionStatus.UPCOMING, ElectionStatus.ACTIVE);
    private static final Set<UserRole> STUDENT_ROLES = Set.of(UserRole.STUDENT, UserRole.VOTER);

    private final VoteRepository voteRepository;
    private final ElectionRepository electionRepository;
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final ElectionService electionService;

    @Transactional
    public void castVote(Long voterId, Long electionId, Long candidateId) {
        User voter = userRepository.findById(voterId)
                .orElseThrow(() -> new IllegalArgumentException("Voter not found."));
        
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new IllegalArgumentException("Election not found."));
        
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found."));

        if (!election.getId().equals(candidate.getElection().getId())) {
            throw new IllegalArgumentException("Candidate does not belong to this election.");
        }

        if (electionService.calculateStatus(election) != ElectionStatus.ACTIVE) {
            throw new IllegalStateException("Voting is only allowed for active elections.");
        }

        if (voteRepository.existsByVoterIdAndElectionId(voterId, electionId)) {
            throw new IllegalStateException("You have already voted in this election.");
        }

        Vote vote = Vote.builder()
                .voter(voter)
                .election(election)
                .candidate(candidate)
                .build();

        voteRepository.save(vote);
    }

    @Transactional(readOnly = true)
    public boolean hasVoted(Long voterId, Long electionId) {
        return voteRepository.existsByVoterIdAndElectionId(voterId, electionId);
    }

    @Transactional(readOnly = true)
    public MyVotesPageDto getMyVotesPage(Long userId, int page, int size) {
        User user = userRepository.findByIdAndRoleIn(userId, STUDENT_ROLES)
                .orElseThrow(() -> new IllegalArgumentException("Student account not found."));

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        LocalDateTime now = LocalDateTime.now();

        Page<VoterElectionHistoryView> historyPage = voteRepository.findMyVoteHistory(
                userId,
                now,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "startsAt"))
        );

        long electionsVoted = voteRepository.countDistinctElectionsVotedByVoterId(userId);
        long votesCast = voteRepository.countByVoterId(userId);
        long upcomingVotes = electionRepository.countUpcomingAvailableForVoter(userId, UPCOMING_ELIGIBLE_STATUSES, now);
        double participationPercentage = toPercentage(electionsVoted, electionsVoted + upcomingVotes);
        long unreadNotifications = userNotificationRepository.countByUserIdAndReadFlagFalse(userId);

        return new MyVotesPageDto(
                user.getFullName(),
                user.getProfileImagePath(),
                user.getUsername(),
                user.getRole().name(),
                unreadNotifications,
                electionsVoted,
                votesCast,
                upcomingVotes,
                participationPercentage,
                historyPage.getContent().stream().map(this::toRowDto).toList(),
                historyPage.getNumber(),
                historyPage.getTotalPages(),
                historyPage.getTotalElements(),
                historyPage.hasPrevious(),
                historyPage.hasNext(),
                Math.max(0, historyPage.getNumber() - 1),
                historyPage.getNumber() + 1
        );
    }

    private MyVoteHistoryRowDto toRowDto(VoterElectionHistoryView row) {
        String status = resolveStatus(row);
        return new MyVoteHistoryRowDto(
                row.getElectionId(),
                row.getElectionTitle(),
                row.getElectionStartsAt(),
                row.getElectionEndsAt(),
                row.getTotalCandidates(),
                row.getSelectedCandidateId(),
                row.getSelectedCandidateName(),
                row.getSelectedCandidateParty(),
                row.getVotedAt(),
                status,
                resolveBadgeClass(status)
        );
    }

    private String resolveStatus(VoterElectionHistoryView row) {
        if (row.getVotedAt() != null) {
            return "VOTED";
        }
        if (row.getElectionStartsAt() != null && LocalDateTime.now().isBefore(row.getElectionStartsAt())) {
            return "UPCOMING";
        }
        return "PENDING";
    }

    private String resolveBadgeClass(String status) {
        return switch (status) {
            case "VOTED" -> "text-bg-success-subtle text-success-emphasis";
            case "UPCOMING" -> "text-bg-warning-subtle text-warning-emphasis";
            default -> "text-bg-primary-subtle text-primary-emphasis";
        };
    }

    private double toPercentage(long value, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(((value * 100.0) / total) * 100.0) / 100.0;
    }
}
