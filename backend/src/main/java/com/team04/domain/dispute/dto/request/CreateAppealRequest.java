package com.team04.domain.dispute.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateAppealRequest(

        @NotBlank String content,
        String fileUrl
) {
}
