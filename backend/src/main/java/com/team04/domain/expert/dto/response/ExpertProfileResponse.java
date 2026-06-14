package com.team04.domain.expert.dto.response;

import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.entity.QualificationType;

public record ExpertProfileResponse(
        Long expertProfileId,
        Long userId,
        QualificationType qualificationType,
        String qualificationNumber,
        boolean verified,
        ExpertStatus status
) {
    public static ExpertProfileResponse from(ExpertProfile profile) {
        return new ExpertProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getQualificationType(),
                profile.getQualificationNumber(),
                profile.isVerified(),
                profile.getStatus()
        );
    }
}