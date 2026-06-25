package com.team04.domain.workspace.repository;

import com.team04.domain.workspace.entity.WorkspaceMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceMessageRepository extends JpaRepository<WorkspaceMessage, Long> {

    List<WorkspaceMessage> findByIdeaIdOrderByCreatedAtAsc(Long ideaId);
}
