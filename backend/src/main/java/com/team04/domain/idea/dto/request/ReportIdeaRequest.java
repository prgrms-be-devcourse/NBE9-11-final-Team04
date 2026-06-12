package com.team04.domain.idea.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 아이디어 도용 의심 신고 사유를 전달하는 요청 DTO입니다. */
public record ReportIdeaRequest(
        @NotBlank
        @Size(max = 1000)
        String reason
) {
}
