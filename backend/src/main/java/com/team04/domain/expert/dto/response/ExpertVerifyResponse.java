package com.team04.domain.expert.dto.response;

import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.ExpertStatus;

public record ExpertVerifyResponse(
        Long expertProfileId,
        boolean verified,
        ExpertStatus status
) {
    public static ExpertVerifyResponse from(ExpertProfile profile) {
        return new ExpertVerifyResponse(
                profile.getId(),
                profile.isVerified(),
                profile.getStatus()
        );
    }
}