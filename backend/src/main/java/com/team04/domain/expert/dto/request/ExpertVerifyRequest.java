package com.team04.domain.expert.dto.request;

import com.team04.domain.expert.entity.QualificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExpertVerifyRequest(

        @NotNull(message = "자격 유형은 필수입니다")
        QualificationType qualificationType,

        @NotBlank(message = "자격 번호는 필수입니다")
        String qualificationNumber,

        // BUSINESS_REGISTRATION 필수 항목
        // YYYYMMDD 포맷의 날짜로 입력
        String startDate,

        // BUSINESS_REGISTRATION 필수 항목
        String representativeName,

        // NATIONAL_QUALIFICATION 필수
        String fileUrl
) {}