package com.team04.domain.match.dto.response;

import com.team04.domain.match.entity.ExpertMatch;
import com.team04.domain.match.entity.MatchStatus;

import java.time.LocalDateTime;

public record ExpertMatchResponse(
        Long matchId,
        Long ideaId,
        Long expertProfileId,
        MatchStatus status,
        LocalDateTime requestedAt,
        LocalDateTime respondedAt,
        String rejectReason
) {
    public static ExpertMatchResponse from(ExpertMatch match) {
        return new ExpertMatchResponse(
                match.getId(),
                match.getIdeaId(),
                match.getExpertProfile().getId(),
                match.getStatus(),
                match.getRequestedAt(),
                match.getRespondedAt(),
                match.getRejectReason()
        );
    }
}