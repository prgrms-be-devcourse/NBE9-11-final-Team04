package com.team04.domain.user.repository;

import com.team04.domain.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile,Long> {
    Optional<Profile> findByUserId(Long userId);
}
