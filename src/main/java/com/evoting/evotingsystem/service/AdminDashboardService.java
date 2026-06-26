package com.evoting.evotingsystem.service;

import com.evoting.evotingsystem.dto.admin.AdminActivityDto;
import com.evoting.evotingsystem.dto.admin.AdminDashboardDto;
import com.evoting.evotingsystem.entity.ElectionStatus;
import com.evoting.evotingsystem.entity.NotificationType;
import com.evoting.evotingsystem.entity.User;
import com.evoting.evotingsystem.entity.UserNotification;
import com.evoting.evotingsystem.entity.UserRole;
import com.evoting.evotingsystem.repository.CandidateRepository;
import com.evoting.evotingsystem.repository.ElectionRepository;
import com.evoting.evotingsystem.repository.UserNotificationRepository;
import com.evoting.evotingsystem.repository.UserRepository;
import com.evoting.evotingsystem.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private static final Set<UserRole> STUDENT_ROLES = Set.of(UserRole.STUDENT, UserRole.VOTER);

    private final UserRepository userRepository;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;
    private final UserNotificationRepository userNotificationRepository;

    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboard(Long adminUserId) {
        User admin = userRepository.findByIdAndRole(adminUserId, UserRole.ADMIN)
                .orElseThrow(() -> new IllegalArgumentException("Admin account not found."));

        long totalVoters = userRepository.countByRoleIn(STUDENT_ROLES);
        long activeElections = electionRepository.countByStatusIn(List.of(ElectionStatus.UPCOMING, ElectionStatus.ACTIVE));
        long totalVotesCast = voteRepository.count();
        long totalCandidates = candidateRepository.count();
        long unreadNotifications = userNotificationRepository.countByUserIdAndReadFlagFalse(adminUserId);

        List<AdminActivityDto> activities = userNotificationRepository.findTop8ByUserIdOrderByCreatedAtDesc(adminUserId)
                .stream()
                .map(this::toActivityDto)
                .toList();

        return new AdminDashboardDto(
                admin.getUsername(),
                admin.getRole().name(),
                unreadNotifications,
                totalVoters,
                activeElections,
                totalVotesCast,
                totalCandidates,
                activities,
                electionRepository.countByStatusIn(List.of(ElectionStatus.COMPLETED)),
                electionRepository.countByStatusIn(List.of(ElectionStatus.UPCOMING)),
                electionRepository.countByStatusIn(List.of(ElectionStatus.ACTIVE))
        );
    }

    private AdminActivityDto toActivityDto(UserNotification notification) {
        NotificationType type = notification.getType() == null ? NotificationType.ANNOUNCEMENT : notification.getType();
        String iconClass = switch (type) {
            case ELECTION -> "bi-calendar2-plus-fill text-primary";
            case VOTE -> "bi-check2-circle text-success";
            case RESULT -> "bi-bar-chart-fill text-warning";
            case SECURITY -> "bi-shield-lock-fill text-danger";
            default -> "bi-megaphone-fill text-info";
        };

        return new AdminActivityDto(
                notification.getTitle(),
                notification.getMessage(),
                iconClass,
                notification.getCreatedAt()
        );
    }
}
