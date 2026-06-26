package com.team04.domain.idea.repository;

import com.team04.domain.idea.entity.IdeaSettlementAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdeaSettlementAccountRepository extends JpaRepository<IdeaSettlementAccount, Long> {

    Optional<IdeaSettlementAccount> findByIdeaId(Long ideaId);
}
