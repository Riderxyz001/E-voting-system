package com.evoting.evotingsystem.service;

import com.evoting.evotingsystem.dto.election.ActiveElectionCardDto;
import com.evoting.evotingsystem.dto.election.ActiveElectionsPageDto;
import com.evoting.evotingsystem.entity.Election;
import com.evoting.evotingsystem.entity.ElectionStatus;
import com.evoting.evotingsystem.entity.User;
import com.evoting.evotingsystem.entity.UserRole;
import com.evoting.evotingsystem.repository.CandidateRepository;
import com.evoting.evotingsystem.repository.ElectionRepository;
import com.evoting.evotingsystem.repository.UserNotificationRepository;
import com.evoting.evotingsystem.repository.UserRepository;
import com.evoting.evotingsystem.repository.projection.ElectionCandidateCountView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ElectionService {
    private static final Set<UserRole> STUDENT_ROLES = Set.of(UserRole.STUDENT, UserRole.VOTER);

    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;
    private final UserNotificationRepository userNotificationRepository;

    @Transactional(readOnly = true)
    public ActiveElectionsPageDto getActiveElectionsPage(
            Long userId,
            String search,
            String statusFilter,
            int page,
            int size
    ) {
        User user = userRepository.findByIdAndRoleIn(userId, STUDENT_ROLES)
                .orElseThrow(() -> new IllegalArgumentException("Student account not found."));

        Set<ElectionStatus> statusSet = resolveStatuses(statusFilter);
        String normalizedSearch = search == null ? "" : search.trim();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.ASC, "startsAt"));

        Page<Election> electionPage;
        if (normalizedSearch.isEmpty()) {
            electionPage = electionRepository.findByStatusIn(statusSet, pageable);
        } else {
            electionPage = electionRepository.findByTitleContainingIgnoreCaseAndStatusIn(normalizedSearch, statusSet, pageable);
        }

        Map<Long, Long> candidateCounts = getCandidateCountMap(electionPage.getContent().stream().map(Election::getId).toList());
        List<ActiveElectionCardDto> cards = electionPage.getContent().stream()
                .map(election -> toCardDto(election, candidateCounts.getOrDefault(election.getId(), 0L)))
                .filter(dto -> !"DRAFT".equals(dto.status()))
                .toList();

        long unreadNotifications = userNotificationRepository.countByUserIdAndReadFlagFalse(userId);

        return new ActiveElectionsPageDto(
                user.getFullName(),
                user.getProfileImagePath(),
                user.getUsername(),
                formatRole(user.getRole()),
                unreadNotifications,
                normalizedSearch,
                statusFilter == null || statusFilter.isBlank() ? "ALL" : statusFilter,
                cards,
                electionPage.getNumber(),
                electionPage.getTotalPages(),
                electionPage.getTotalElements(),
                electionPage.hasPrevious(),
                electionPage.hasNext(),
                Math.max(0, electionPage.getNumber() - 1),
                electionPage.getNumber() + 1
        );
    }

    private Map<Long, Long> getCandidateCountMap(List<Long> electionIds) {
        if (electionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return candidateRepository.findCandidateCountsByElectionIds(electionIds)
                .stream()
                .collect(Collectors.toMap(ElectionCandidateCountView::getElectionId, ElectionCandidateCountView::getCandidateCount));
    }

    private ActiveElectionCardDto toCardDto(Election election, long totalCandidates) {
        ElectionStatus status = calculateStatus(election);
        String displayStatus = status.name();
        String badgeClass = switch (status) {
            case ACTIVE -> "text-bg-success-subtle text-success-emphasis";
            case UPCOMING -> "text-bg-warning-subtle text-warning-emphasis";
            default -> "text-bg-secondary-subtle text-secondary-emphasis";
        };
        long daysLeft = calculateDaysLeft(election.getEndsAt());

        return new ActiveElectionCardDto(
                election.getId(),
                election.getTitle(),
                election.getDescription(),
                election.getStartsAt(),
                election.getEndsAt(),
                displayStatus,
                badgeClass,
                totalCandidates,
                daysLeft
        );
    }

    public ElectionStatus calculateStatus(Election election) {
        if (election.getStatus() == ElectionStatus.DRAFT || election.getStatus() == ElectionStatus.CANCELLED) {
            return election.getStatus();
        }

        // Check candidate count - minimum 2 required to move out of DRAFT
        long candidateCount = candidateRepository.countByElectionId(election.getId());
        if (candidateCount < 2) {
            return ElectionStatus.DRAFT;
        }
        
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        LocalDateTime startsAt = election.getStartsAt();
        LocalDateTime endsAt = election.getEndsAt();

        if (startsAt == null || endsAt == null) {
            return election.getStatus();
        }

        if (now.isBefore(startsAt)) {
            return ElectionStatus.UPCOMING;
        } else if (now.isAfter(endsAt)) {
            return ElectionStatus.COMPLETED;
        } else {
            return ElectionStatus.ACTIVE;
        }
    }

    private String computeDisplayStatus(LocalDateTime startsAt, LocalDateTime endsAt, ElectionStatus fallbackStatus) {
        Election mockElection = Election.builder()
                .startsAt(startsAt)
                .endsAt(endsAt)
                .status(fallbackStatus)
                .build();
        return calculateStatus(mockElection).name();
    }

    private long calculateDaysLeft(LocalDateTime endsAt) {
        if (endsAt == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), endsAt.toLocalDate());
        return Math.max(days, 0);
    }

    private Set<ElectionStatus> resolveStatuses(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter)) {
            return Set.of(ElectionStatus.UPCOMING, ElectionStatus.ACTIVE, ElectionStatus.COMPLETED);
        }
        try {
            String filter = statusFilter.toUpperCase();
            if ("ONGOING".equals(filter)) filter = "ACTIVE";
            
            ElectionStatus status = ElectionStatus.valueOf(filter);
            if (status == ElectionStatus.DRAFT || status == ElectionStatus.CANCELLED) {
                return Set.of(ElectionStatus.UPCOMING, ElectionStatus.ACTIVE, ElectionStatus.COMPLETED);
            }
            return Set.of(status);
        } catch (IllegalArgumentException ex) {
            return Set.of(ElectionStatus.UPCOMING, ElectionStatus.ACTIVE, ElectionStatus.COMPLETED);
        }
    }

    private String formatRole(UserRole role) {
        if (role == UserRole.ADMIN) return "ADMIN";
        return "VOTER";
    }
}
