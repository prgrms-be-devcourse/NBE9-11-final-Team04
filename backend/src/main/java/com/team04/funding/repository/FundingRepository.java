package com.team04.funding.repository;

import com.team04.funding.entity.Funding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FundingRepository extends JpaRepository<Funding, Long> {

    List<Funding> findByIdeaIdOrderByCreatedAtDesc(Long ideaId);
}
