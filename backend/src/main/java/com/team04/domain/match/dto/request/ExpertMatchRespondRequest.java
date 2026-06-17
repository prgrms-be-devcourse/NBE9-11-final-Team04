package com.team04.domain.match.dto.request;

import com.team04.domain.match.entity.MatchStatus;
import jakarta.validation.constraints.NotNull;

public record ExpertMatchRespondRequest(
        // 수락/거절 요청 바디

        @NotNull(message = "응답 상태는 필수입니다")
        MatchStatus status,  // ACCEPTED 또는 REJECTED

        String rejectReason  // REJECTED 시 필수
) {}