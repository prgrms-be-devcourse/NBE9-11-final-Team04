package com.team04.domain.milestone.repository;

import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.entity.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByIdeaIdOrderByStep(Long ideaId);

    Optional<Milestone> findByIdeaIdAndStep(Long ideaId, Integer step);

    Optional<Milestone> findByIdeaIdAndStatus(Long ideaId, MilestoneStatus status);
}