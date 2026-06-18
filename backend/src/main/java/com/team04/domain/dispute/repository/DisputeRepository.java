package com.team04.domain.dispute.repository;

import com.team04.domain.dispute.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
}
