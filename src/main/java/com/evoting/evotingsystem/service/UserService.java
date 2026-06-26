package com.evoting.evotingsystem.service;

import com.evoting.evotingsystem.dto.profile.ProfilePageDto;
import com.evoting.evotingsystem.dto.profile.ChangePasswordRequest;
import com.evoting.evotingsystem.dto.profile.UpdateProfileRequest;
import com.evoting.evotingsystem.entity.ElectionStatus;
import com.evoting.evotingsystem.entity.User;
import com.evoting.evotingsystem.entity.UserRole;
import com.evoting.evotingsystem.repository.ElectionRepository;
import com.evoting.evotingsystem.repository.UserNotificationRepository;
import com.evoting.evotingsystem.repository.UserRepository;
import com.evoting.evotingsystem.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Set<UserRole> STUDENT_ROLES = Set.of(UserRole.STUDENT, UserRole.VOTER);

    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final ElectionRepository electionRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public ProfilePageDto getProfilePage(Long userId) {
        User user = getVoterById(userId);
        long electionsVoted = voteRepository.countDistinctElectionsVotedByVoterId(userId);
        long votesCast = voteRepository.countByVoterId(userId);
        long openElections = electionRepository.countByStatusIn(List.of(ElectionStatus.UPCOMING, ElectionStatus.ACTIVE));
        long unreadNotifications = userNotificationRepository.countByUserIdAndReadFlagFalse(userId);
        double participation = toPercentage(electionsVoted, Math.max(openElections, electionsVoted));

        return new ProfilePageDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getProfileImagePath(),
                user.getRole().name(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getCreatedAt(),
                unreadNotifications,
                electionsVoted,
                votesCast,
                participation,
                user.getAccountStatus(),
                "VOTR-" + String.format("%05d", user.getId()),
                user.getLastLoginAt(),
                Boolean.TRUE.equals(user.getTwoFactorEnabled()),
                Boolean.TRUE.equals(user.getLoginNotificationsEnabled())
        );
    }

    @Transactional(readOnly = true)
    public UpdateProfileRequest getUpdateProfileRequest(Long userId) {
        User user = getVoterById(userId);
        return UpdateProfileRequest.builder()
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .build();
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getVoterById(userId);
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedUsername = request.getUsername().trim();

        if (userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
            throw new IllegalArgumentException("Email is already registered with another account.");
        }
        if (userRepository.existsByUsernameAndIdNot(normalizedUsername, userId)) {
            throw new IllegalArgumentException("Username is already taken by another user.");
        }

        user.setFullName(request.getFullName().trim());
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPhoneNumber(blankToNull(request.getPhoneNumber()));
        user.setAddress(blankToNull(request.getAddress()));
        user.setGender(blankToNull(request.getGender()));
        user.setDateOfBirth(request.getDateOfBirth());

        String newPhotoPath = fileStorageService.storeProfilePhoto(request.getProfilePhoto());
        if (newPhotoPath != null) {
            user.setProfileImagePath(newPhotoPath);
        }
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getVoterById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match.");
        }
        validatePasswordStrength(request.getNewPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User getVoterById(Long userId) {
        return userRepository.findByIdAndRoleIn(userId, STUDENT_ROLES)
                .orElseThrow(() -> new IllegalArgumentException("Student account not found."));
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private double toPercentage(long value, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(((value * 100.0) / total) * 100.0) / 100.0;
    }

    private void validatePasswordStrength(String password) {
        if (password == null) {
            throw new IllegalArgumentException("New password is required.");
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!(hasUpper && hasLower && hasDigit && hasSpecial)) {
            throw new IllegalArgumentException("Password must contain uppercase, lowercase, number, and special character.");
        }
    }
}
