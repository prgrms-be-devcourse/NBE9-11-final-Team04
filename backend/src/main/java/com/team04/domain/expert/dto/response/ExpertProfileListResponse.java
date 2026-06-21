package com.team04.domain.expert.dto.response;

import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.TechStack;

public record ExpertProfileListResponse(
        Long expertProfileId,
        Long userId,
        String name,
        String nickname,
        TechStack techStack,
        String career
) {
    public static ExpertProfileListResponse from(ExpertProfile profile) {
        return new ExpertProfileListResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getName(),
                profile.getUser().getNickname(),
                profile.getTechStack(),
                profile.getCareer()
        );
    }
}