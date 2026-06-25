package com.team04.domain.expert.repository;

import com.team04.domain.expert.entity.ExpertProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, Long>, ExpertProfileRepositoryCustom{
    boolean existsByUserId(Long userId);
    Optional<ExpertProfile> findByUserId(Long userId);
}
