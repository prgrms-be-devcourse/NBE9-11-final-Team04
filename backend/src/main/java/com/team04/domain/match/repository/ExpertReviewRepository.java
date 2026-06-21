package com.team04.domain.match.repository;

import com.team04.domain.match.entity.ExpertReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpertReviewRepository extends JpaRepository<ExpertReview, Long> {

    boolean existsByExpertMatch_Id(Long matchId);
}
