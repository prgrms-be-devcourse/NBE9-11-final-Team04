package com.team04.domain.payment.repository;

import com.team04.domain.payment.entity.IdeaVbankPool;
import com.team04.domain.payment.entity.IdeaVbankPoolStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdeaVbankPoolRepository extends JpaRepository<IdeaVbankPool, Long> {

    Optional<IdeaVbankPool> findByIdeaIdAndStatus(Long ideaId, IdeaVbankPoolStatus status);

    Optional<IdeaVbankPool> findByPoolOrderId(String poolOrderId);
}
