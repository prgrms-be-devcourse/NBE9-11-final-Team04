package com.team04.domain.expert.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ExpertAppealRequest(

        @NotBlank(message = "소명 내용은 필수입니다")
        String content
) {}