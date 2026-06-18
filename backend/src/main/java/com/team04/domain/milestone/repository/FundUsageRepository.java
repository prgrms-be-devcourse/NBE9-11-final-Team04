package com.team04.domain.milestone.repository;

import com.team04.domain.milestone.entity.FundUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FundUsageRepository extends JpaRepository<FundUsage, Long> {

    List<FundUsage> findByIdeaIdOrderByUsedAtDesc(Long ideaId);

    /** 프로젝트의 총 자금 사용 금액 조회 */
    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM FundUsage f WHERE f.ideaId = :ideaId")
    Long sumAmountByIdeaId(@Param("ideaId") Long ideaId);
}