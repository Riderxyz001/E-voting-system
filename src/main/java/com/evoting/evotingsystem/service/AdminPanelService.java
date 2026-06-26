package com.evoting.evotingsystem.service;

import com.evoting.evotingsystem.entity.Candidate;
import com.evoting.evotingsystem.entity.Election;
import com.evoting.evotingsystem.entity.ElectionStatus;
import com.evoting.evotingsystem.entity.NotificationType;
import com.evoting.evotingsystem.entity.SystemSetting;
import com.evoting.evotingsystem.entity.User;
import com.evoting.evotingsystem.entity.UserNotification;
import com.evoting.evotingsystem.entity.UserRole;
import com.evoting.evotingsystem.repository.CandidateRepository;
import com.evoting.evotingsystem.repository.ElectionRepository;
import com.evoting.evotingsystem.repository.SystemSettingRepository;
import com.evoting.evotingsystem.repository.UserNotificationRepository;
import com.evoting.evotingsystem.repository.UserRepository;
import com.evoting.evotingsystem.repository.VoteRepository;
import com.evoting.evotingsystem.repository.projection.AdminVoteRowView;
import com.evoting.evotingsystem.repository.projection.CandidateVoteAggregateView;
import com.evoting.evotingsystem.repository.projection.ElectionCandidateCountView;
import com.evoting.evotingsystem.repository.projection.ElectionVoteTotalView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
public class AdminPanelService {
    private static final Set<UserRole> STUDENT_ROLES = Set.of(UserRole.STUDENT, UserRole.VOTER);

    private final UserRepository userRepository;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final SystemSettingRepository systemSettingRepository;

    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public User getAdmin(Long userId) {
        return userRepository.findByIdAndRole(userId, UserRole.ADMIN)
                .orElseThrow(() -> new IllegalArgumentException("Admin account not found."));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getShellData(Long adminId) {
        User admin = getAdmin(adminId);
        return Map.of(
                "username", admin.getUsername(),
                "role", admin.getRole().name(),
                "unreadNotifications", userNotificationRepository.countByUserIdAndReadFlagFalse(adminId)
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getElectionsPage(String q, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        String search = q == null ? "" : q.trim();
        Set<ElectionStatus> statuses = resolveElectionStatuses(status);

        Page<Election> data = search.isBlank()
                ? electionRepository.findByStatusIn(statuses, pageable)
                : electionRepository.findByTitleContainingIgnoreCaseAndStatusIn(search, statuses, pageable);

        List<Long> ids = data.getContent().stream().map(Election::getId).toList();
        Map<Long, Long> candidates = candidateRepository.findCandidateCountsByElectionIds(ids)
                .stream().collect(Collectors.toMap(ElectionCandidateCountView::getElectionId, ElectionCandidateCountView::getCandidateCount));
        Map<Long, Long> votes = voteRepository.findVoteTotalsForElectionIds(ids)
                .stream().collect(Collectors.toMap(ElectionVoteTotalView::getElectionId, ElectionVoteTotalView::getTotalVotes));

        return Map.of(
                "elections", data,
                "candidateCountMap", candidates,
                "voteCountMap", votes,
                "search", search,
                "status", status == null || status.isBlank() ? "ALL" : status
        );
    }

    @Transactional
    public void createElection(String title, String description, LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt == null || endsAt == null) {
            throw new IllegalArgumentException("Start and end date are required.");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("End date must be after start date.");
        }
        Election election = Election.builder()
                .title(title.trim())
                .description(description.trim())
                .startsAt(startsAt)
                .endsAt(endsAt)
                .status(ElectionStatus.DRAFT)
                .build();
        electionRepository.save(election);
    }

    @Transactional
    public void deleteElection(Long id) {
        electionRepository.deleteById(id);
    }

    @Transactional
    public void publishElection(Long id) {
        Election election = electionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Election not found."));

        if (election.getCandidates().size() < 2) {
            throw new IllegalArgumentException("At least 2 candidates are required before activating an election.");
        }

        election.setStatus(ElectionStatus.ACTIVE);
        electionRepository.save(election);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCandidatesPage(String q, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        String search = q == null ? "" : q.trim();
        Page<Candidate> candidates = search.isBlank()
                ? candidateRepository.findAll(pageable)
                : candidateRepository.findByFullNameContainingIgnoreCase(search, pageable);

        Map<Long, Long> voteCountByCandidate = candidateRepository.findCandidateVotesForElectionIds(
                        candidates.getContent().stream().map(c -> c.getElection().getId()).distinct().toList()
                ).stream().collect(Collectors.toMap(CandidateVoteAggregateView::getCandidateId, CandidateVoteAggregateView::getVoteCount, Long::sum));

        return Map.of("candidates", candidates, "voteCountByCandidate", voteCountByCandidate, "search", search);
    }

    @Transactional
    public void createCandidate(String fullName, String partyName, String manifesto, Long electionId, org.springframework.web.multipart.MultipartFile photo) {
        if (electionId == null) {
            throw new IllegalArgumentException("Please select an election.");
        }
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new IllegalArgumentException("Election not found"));

        String photoPath = fileStorageService.storeCandidatePhoto(photo);

        Candidate candidate = Candidate.builder()
                .fullName(fullName.trim())
                .partyName(partyName.trim())
                .manifesto(manifesto == null ? null : manifesto.trim())
                .election(election)
                .imagePath(photoPath)
                .build();
        candidateRepository.save(candidate);
    }

    @Transactional
    public void deleteCandidate(Long id) {
        candidateRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<User> getVotersPage(String q, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        String search = q == null ? "" : q.trim();
        if (search.isBlank()) {
            return userRepository.findByRoleIn(STUDENT_ROLES, pageable);
        }
        return userRepository.findByRoleInAndFullNameContainingIgnoreCase(STUDENT_ROLES, search, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdminVoteRowView> getVotesPage(Long electionId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "votedAt"));
        return voteRepository.findAdminVoteRows(electionId, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getResultsPage() {
        List<Election> completed = electionRepository.findTop5ByStatusOrderByEndsAtDesc(ElectionStatus.COMPLETED);
        List<Long> ids = completed.stream().map(Election::getId).toList();
        Map<Long, List<CandidateVoteAggregateView>> candidateVotes = ids.isEmpty()
                ? Collections.emptyMap()
                : candidateRepository.findCandidateVotesForElectionIds(ids).stream()
                .collect(Collectors.groupingBy(CandidateVoteAggregateView::getElectionId));
        Map<Long, Long> voteTotals = ids.isEmpty()
                ? Collections.emptyMap()
                : voteRepository.findVoteTotalsForElectionIds(ids).stream()
                .collect(Collectors.toMap(ElectionVoteTotalView::getElectionId, ElectionVoteTotalView::getTotalVotes));

        List<String> chartLabels = completed.stream().map(Election::getTitle).toList();
        List<Long> chartValues = completed.stream().map(e -> voteTotals.getOrDefault(e.getId(), 0L)).toList();
        long totalVotes = chartValues.stream().mapToLong(Long::longValue).sum();

        return Map.of(
                "completedElections", completed,
                "candidateVotes", candidateVotes,
                "voteTotals", voteTotals,
                "chartLabels", chartLabels,
                "chartValues", chartValues,
                "totalVotes", totalVotes
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminNotificationsPage(Long adminId, String type, int page, int size) {
        NotificationType selected = resolveNotificationType(type);
        Page<UserNotification> notifications = selected == null
                ? userNotificationRepository.findByUserIdOrderByCreatedAtDesc(adminId, PageRequest.of(Math.max(0, page), Math.max(1, size)))
                : userNotificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(adminId, selected, PageRequest.of(Math.max(0, page), Math.max(1, size)));
        return Map.of("notifications", notifications, "selectedType", selected == null ? "ALL" : selected.name());
    }

    @Transactional
    public void sendNotificationToAllVoters(Long adminId, String title, String message, NotificationType type) {
        User admin = getAdmin(adminId);
        List<User> voters = userRepository.findByRoleIn(STUDENT_ROLES, Pageable.unpaged()).getContent();
        List<UserNotification> voterNotifications = voters.stream()
                .map(voter -> UserNotification.builder()
                        .user(voter)
                        .title(title.trim())
                        .message(message.trim())
                        .type(type)
                        .redirectUrl("/voter/notifications")
                        .readFlag(false)
                        .build())
                .toList();
        userNotificationRepository.saveAll(voterNotifications);
        userNotificationRepository.save(UserNotification.builder()
                .user(admin)
                .title("Notification Sent")
                .message("Broadcast sent to " + voters.size() + " voters.")
                .type(NotificationType.ANNOUNCEMENT)
                .readFlag(false)
                .build());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReportsPage() {
        long voters = userRepository.countByRoleIn(STUDENT_ROLES);
        long votes = voteRepository.count();
        long elections = electionRepository.count();
        double turnout = voters == 0 ? 0.0 : Math.round(((votes * 100.0) / voters) * 100.0) / 100.0;
        List<Election> recent = electionRepository.findTop5ByStatusOrderByEndsAtDesc(ElectionStatus.COMPLETED);
        return Map.of("totalVoters", voters, "totalVotes", votes, "totalElections", elections, "turnout", turnout, "recentElections", recent);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUsersPage(String q, String role, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        String search = q == null ? "" : q.trim();
        Page<User> users;
        if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
            UserRole selected = UserRole.valueOf(role.toUpperCase());
            users = search.isBlank()
                    ? userRepository.findByRole(selected, pageable)
                    : userRepository.findByRoleAndFullNameContainingIgnoreCase(selected, search, pageable);
        } else {
            users = search.isBlank() ? userRepository.findAll(pageable) : userRepository.findByFullNameContainingIgnoreCase(search, pageable);
        }
        return Map.of("users", users, "search", search, "role", role == null || role.isBlank() ? "ALL" : role);
    }

    @Transactional
    public void updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setAccountStatus(status);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<SystemSetting> getSettings() {
        ensureDefaultSettings();
        return systemSettingRepository.findAll(Sort.by("settingKey"));
    }

    @Transactional
    public void saveSetting(String key, String value) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElse(SystemSetting.builder().settingKey(key).description("System setting").build());
        setting.setSettingValue(value);
        systemSettingRepository.save(setting);
    }

    private void ensureDefaultSettings() {
        List<String> defaults = List.of("site_name", "maintenance_mode", "two_factor_required", "backup_frequency");
        for (String key : defaults) {
            systemSettingRepository.findBySettingKey(key).orElseGet(() ->
                    systemSettingRepository.save(SystemSetting.builder()
                            .settingKey(key)
                            .settingValue("site_name".equals(key) ? "E-Voting System" : "false")
                            .description("Default setting")
                            .build())
            );
        }
    }

    private Set<ElectionStatus> resolveElectionStatuses(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return Set.of(ElectionStatus.UPCOMING, ElectionStatus.ACTIVE, ElectionStatus.COMPLETED, ElectionStatus.DRAFT, ElectionStatus.CANCELLED);
        }
        try {
            return Set.of(ElectionStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return Set.of(ElectionStatus.UPCOMING, ElectionStatus.ACTIVE, ElectionStatus.COMPLETED, ElectionStatus.DRAFT, ElectionStatus.CANCELLED);
        }
    }

    private NotificationType resolveNotificationType(String type) {
        if (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type)) {
            return null;
        }
        try {
            return NotificationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
