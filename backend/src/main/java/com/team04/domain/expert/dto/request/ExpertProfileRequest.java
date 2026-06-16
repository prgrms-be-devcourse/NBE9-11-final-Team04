package com.team04.domain.expert.dto.request;

import com.team04.domain.expert.entity.TechStack;
import jakarta.validation.constraints.NotNull;

public record ExpertProfileRequest(

        @NotNull(message = "기술 스택은 필수입니다.")
        TechStack techStack,

        String portfolioUrl,

        String career
) {}