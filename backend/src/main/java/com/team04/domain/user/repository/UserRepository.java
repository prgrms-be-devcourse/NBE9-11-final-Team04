package com.team04.domain.user.repository;

import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.status.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndStatus(String email, UserStatus status);

    List<User> findByRole(Role role);
}