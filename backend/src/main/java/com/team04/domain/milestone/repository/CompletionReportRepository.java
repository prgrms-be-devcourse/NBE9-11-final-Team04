package com.team04.domain.milestone.repository;

import com.team04.domain.milestone.entity.CompletionReport;
import com.team04.domain.milestone.entity.CompletionReportStatus;
import com.team04.domain.milestone.entity.CompletionReportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompletionReportRepository extends JpaRepository<CompletionReport, Long> {

    boolean existsByMilestoneIdAndStatus(Long milestoneId, CompletionReportStatus status);

    Optional<CompletionReport> findByMilestoneIdAndType(Long milestoneId, CompletionReportType type);

    Optional<CompletionReport> findTopByMilestoneIdAndTypeOrderBySubmittedAtDesc(
            Long milestoneId,
            CompletionReportType type
    );

    Optional<CompletionReport> findTopByMilestoneIdOrderBySubmittedAtDesc(Long milestoneId);

    long countByMilestoneIdAndType(Long milestoneId, CompletionReportType type);

    /** 마일스톤 단건의 완료/소명 보고서 전체 조회 */
    List<CompletionReport> findByMilestoneIdOrderBySubmittedAtDesc(Long milestoneId);
}
