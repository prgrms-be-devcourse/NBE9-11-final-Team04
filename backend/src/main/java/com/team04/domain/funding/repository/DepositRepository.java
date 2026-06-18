package com.team04.domain.funding.repository;

import com.team04.domain.funding.entity.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepositRepository extends JpaRepository<Deposit, Long> {

    Optional<Deposit> findByIdeaId(Long ideaId);
}
