package com.team04.domain.idea.repository;

import com.team04.domain.idea.entity.IdeaSettlementAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdeaSettlementAccountRepository extends JpaRepository<IdeaSettlementAccount, Long> {

    Optional<IdeaSettlementAccount> findByIdeaId(Long ideaId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM IdeaSettlementAccount a WHERE a.ideaId = :ideaId")
    int deleteByIdeaIdBulk(@Param("ideaId") Long ideaId);
}
