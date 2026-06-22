package com.team04.domain.dispute.repository;

import com.team04.domain.dispute.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    @Query("SELECT d FROM Dispute d JOIN FETCH d.idea JOIN FETCH d.reporter JOIN FETCH d.proposer WHERE d.id = :id")
    Optional<Dispute> findByIdWithDetails(@Param("id") Long id);
}
