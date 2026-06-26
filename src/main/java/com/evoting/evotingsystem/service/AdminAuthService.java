package com.evoting.evotingsystem.service;

import com.evoting.evotingsystem.entity.User;
import com.evoting.evotingsystem.entity.UserRole;
import com.evoting.evotingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User authenticateAdmin(String email, String rawPassword) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .filter(user -> user.getPassword().equals(rawPassword))
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .orElse(null);
    }
}
