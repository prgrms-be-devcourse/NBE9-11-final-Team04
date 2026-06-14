package com.team04.domain.expert.dto.request;

import com.team04.domain.expert.entity.QualificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExpertProfileRequest(

        @NotNull(message = "자격 유형은 필수입니다")
        QualificationType qualificationType,

        @NotBlank(message = "자격 번호는 필수입니다")
        String qualificationNumber
) {}