package com.team04.domain.match.dto.request;

import jakarta.validation.constraints.NotNull;

public record MatchRequest(
        @NotNull(message = "아이디어 ID는 필수입니다")
        Long ideaId
) {}