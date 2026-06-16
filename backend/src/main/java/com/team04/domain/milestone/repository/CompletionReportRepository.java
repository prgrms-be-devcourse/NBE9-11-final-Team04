package com.team04.domain.milestone.repository;

import com.team04.domain.milestone.entity.CompletionReport;
import com.team04.domain.milestone.entity.CompletionReportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompletionReportRepository extends JpaRepository<CompletionReport, Long> {

    Optional<CompletionReport> findByMilestoneIdAndType(Long milestoneId, CompletionReportType type);
}