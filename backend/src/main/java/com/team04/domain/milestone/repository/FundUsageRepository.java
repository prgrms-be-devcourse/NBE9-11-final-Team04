package com.team04.domain.milestone.repository;

import com.team04.domain.milestone.entity.FundUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FundUsageRepository extends JpaRepository<FundUsage, Long> {

    List<FundUsage> findByIdeaIdOrderByUsedAtDesc(Long ideaId);
}