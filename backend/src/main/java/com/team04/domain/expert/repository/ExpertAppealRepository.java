package com.team04.domain.expert.repository;

import com.team04.domain.expert.entity.ExpertAppeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpertAppealRepository extends JpaRepository<ExpertAppeal, Long> {

    List<ExpertAppeal> findByExpertProfileIdOrderBySubmittedAtDesc(Long expertProfileId);
}