package com.evoting.evotingsystem.repository;

import com.evoting.evotingsystem.entity.User;
import com.evoting.evotingsystem.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByUsernameAndIdNot(String username, Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndRole(Long id, UserRole role);

    Optional<User> findByIdAndRoleIn(Long id, Collection<UserRole> roles);

    long countByRole(UserRole role);

    long countByRoleIn(Collection<UserRole> roles);

    long countByRoleAndAccountStatusIgnoreCase(UserRole role, String accountStatus);

    Page<User> findByRole(UserRole role, Pageable pageable);

    Page<User> findByRoleIn(Collection<UserRole> roles, Pageable pageable);

    Page<User> findByRoleAndFullNameContainingIgnoreCase(UserRole role, String fullName, Pageable pageable);

    Page<User> findByRoleInAndFullNameContainingIgnoreCase(Collection<UserRole> roles, String fullName, Pageable pageable);

    Page<User> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);
}
