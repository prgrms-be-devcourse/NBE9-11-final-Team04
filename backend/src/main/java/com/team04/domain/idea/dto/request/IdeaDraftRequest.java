package com.team04.domain.idea.dto.request;

import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.RewardType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** 아이디어 임시저장을 생성하거나 수정하기 위한 요청 DTO입니다. */
public record IdeaDraftRequest(
        @Size(max = 100) String title,
        IdeaCategory category,
        @Size(max = 200) String oneLineIntro,
        @Size(max = 5000) String problemDefinition,
        @Size(max = 5000) String solution,
        @Size(max = 5000) String goal,
        @Size(max = 5000) String targetCustomer,
        @Size(max = 5000) String competitor,
        @Size(max = 5000) String teamIntro,
        @Min(1_000_000) @Max(100_000_000) Long goalAmount,
        LocalDateTime fundingStartAt,
        LocalDateTime fundingEndAt,
        RewardType rewardType
) {
}
