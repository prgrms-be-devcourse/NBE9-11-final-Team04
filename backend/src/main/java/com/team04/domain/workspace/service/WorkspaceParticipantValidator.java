package com.team04.domain.workspace.service;

import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceParticipantValidator {

    private final IdeaRepository ideaRepository;
    private final FundingRepository fundingRepository;

    public Idea getIdeaOrThrow(Long workspaceId) {
        return ideaRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
    }

    public void validateParticipant(Long workspaceId, Long userId, Role role) {
        if (role == Role.ADMIN) {
            return;
        }

        Idea idea = getIdeaOrThrow(workspaceId);

        if (idea.getUserId().equals(userId)) {
            return;
        }

        if (fundingRepository.existsPaidSponsorByIdeaIdAndSponsorId(workspaceId, userId)) {
            return;
        }

        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    public boolean isCreator(Idea idea, Long userId) {
        return idea.getUserId().equals(userId);
    }
}
