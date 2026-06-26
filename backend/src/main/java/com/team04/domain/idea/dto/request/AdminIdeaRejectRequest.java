package com.team04.domain.idea.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 관리자가 아이디어를 반려할 때 입력하는 반려 사유 요청 DTO입니다. */
public record AdminIdeaRejectRequest(
        @NotBlank String reason
) {
}
