package com.team04.domain.dispute.service;

import com.team04.domain.dispute.entity.TargetType;
import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.match.entity.ExpertMatch;
import com.team04.domain.match.repository.ExpertMatchRepository;
import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DisputeParticipantValidator {

    private final IdeaRepository ideaRepository;
    private final FundingRepository fundingRepository;
    private final MilestoneRepository milestoneRepository;
    private final ExpertMatchRepository expertMatchRepository;

    public void validate(TargetType targetType, Long targetId, Long reportedUserId) {
        boolean valid = switch (targetType) {
            case IDEA -> {
                Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(targetId)
                        .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
                yield idea.getUserId().equals(reportedUserId);
            }
            case FUNDING -> {
                Funding funding = fundingRepository.findById(targetId)
                        .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));
                Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(funding.getIdeaId())
                        .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
                yield funding.getSponsorId().equals(reportedUserId) || idea.getUserId().equals(reportedUserId);
            }
            case MILESTONE -> {
                Milestone milestone = milestoneRepository.findById(targetId)
                        .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
                Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(milestone.getIdeaId())
                        .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
                yield idea.getUserId().equals(reportedUserId);
            }
            case EXPERT_MATCH -> {
                ExpertMatch match = expertMatchRepository.findByIdWithProfile(targetId)
                        .orElseThrow(() -> new CustomException(ErrorCode.MATCH_NOT_FOUND));
                Long expertUserId = match.getExpertProfile().getUser().getId();
                Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(match.getIdeaId())
                        .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
                yield expertUserId.equals(reportedUserId) || idea.getUserId().equals(reportedUserId);
            }
            case USER -> targetId.equals(reportedUserId);
        };

        if (!valid) {
            throw new CustomException(ErrorCode.DISPUTE_REPORTED_NOT_PARTICIPANT);
        }
    }
}
