package com.team04.domain.workspace.dto.response;

import com.team04.domain.user.entity.User;
import com.team04.domain.workspace.entity.WorkspaceMessage;

import java.time.LocalDateTime;

public record WorkspaceMessageResponse(
        Long messageId,
        Long authorId,
        String authorNickname,
        String content,
        LocalDateTime createdAt
) {
    public static WorkspaceMessageResponse from(WorkspaceMessage message, User author) {
        return new WorkspaceMessageResponse(
                message.getId(),
                message.getAuthorId(),
                author.getNickname(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
