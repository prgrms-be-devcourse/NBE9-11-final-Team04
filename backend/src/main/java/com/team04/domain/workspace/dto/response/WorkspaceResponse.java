package com.team04.domain.workspace.dto.response;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.user.entity.User;

public record WorkspaceResponse(
        Long workspaceId,
        Long ideaId,
        String title,
        String status,
        Long creatorId,
        String creatorNickname,
        boolean creator
) {
    public static WorkspaceResponse of(Idea idea, User creator, Long currentUserId) {
        return new WorkspaceResponse(
                idea.getId(),
                idea.getId(),
                idea.getTitle(),
                idea.getStatus().name(),
                idea.getUserId(),
                creator.getNickname(),
                idea.getUserId().equals(currentUserId)
        );
    }
}