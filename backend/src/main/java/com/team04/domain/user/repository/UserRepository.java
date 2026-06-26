package com.team04.domain.user.repository;

import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.status.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    List<User> findByEmailOrNickname(String email, String nickname);

    List<User> findByRoleAndStatus(Role role, UserStatus status);

    List<User> findByStatus(UserStatus status);

    Page<User> findAll(Pageable pageable);

    @Query("""
      SELECT COUNT(u) FROM User u
      WHERE u.role = :role AND u.status = 'ACTIVE'
      """)
    long countByRoleAndActive(@Param("role") Role role);

    long countByStatus(UserStatus status);
}