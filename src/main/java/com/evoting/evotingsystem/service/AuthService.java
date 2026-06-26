package com.evoting.evotingsystem.service;

import com.evoting.evotingsystem.dto.RegistrationRequest;
import com.evoting.evotingsystem.entity.User;
import com.evoting.evotingsystem.entity.UserRole;
import com.evoting.evotingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @Transactional
    public void registerVoter(RegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match.");
        }

        String profileImagePath = fileStorageService.storeProfilePhoto(request.getProfilePhoto());

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.STUDENT)
                .profileImagePath(profileImagePath)
                .build();

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean authenticate(String email, String rawPassword) {
        return authenticateAndGetUser(email, rawPassword) != null;
    }

    @Transactional
    public User authenticateAndGetUser(String email, String rawPassword) {
        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null) {
            return null;
        }

        boolean matches;
        String stored = user.getPassword();
        if (stored != null && stored.startsWith("$2")) {
            matches = passwordEncoder.matches(rawPassword, stored);
        } else {
            matches = stored != null && stored.equals(rawPassword);
            if (matches) {
                user.setPassword(passwordEncoder.encode(rawPassword));
            }
        }

        if (!matches) {
            return null;
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return user;
    }
}
