package com.team04.domain.expert.dto.response;

import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.entity.QualificationType;
import com.team04.domain.expert.entity.TechStack;

public record ExpertProfileResponse(
        Long expertProfileId,
        Long userId,
        QualificationType qualificationType,
        String qualificationNumber,
        boolean verified,
        ExpertStatus status,
        TechStack techStack,
        String portfolioUrl,
        String career

) {
    public static ExpertProfileResponse from(ExpertProfile profile) {
        return new ExpertProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getQualificationType(),
                profile.getQualificationNumber(),
                profile.isVerified(),
                profile.getStatus(),
                profile.getTechStack(),
                profile.getPortfolioUrl(),
                profile.getCareer()
        );
    }
}