package com.team04.domain.dispute.repository;

import com.team04.domain.dispute.entity.DisputeAppeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DisputeAppealRepository extends JpaRepository<DisputeAppeal, Long> {
    boolean existsByDisputeId(Long disputeId);
    Optional<DisputeAppeal> findByDisputeId(Long disputeId);
}
