package com.team04.domain.expert.dto.response;

import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.entity.QualificationType;

public record ExpertVerifyResponse(
        Long expertProfileId,
        boolean verified,
        ExpertStatus status,
        QualificationType qualificationType,
        String qualificationNumber
) {
    public static ExpertVerifyResponse from(ExpertProfile profile) {
        return new ExpertVerifyResponse(
                profile.getId(),
                profile.isVerified(),
                profile.getStatus(),
                profile.getQualificationType(),
                profile.getQualificationNumber()
        );
    }
}