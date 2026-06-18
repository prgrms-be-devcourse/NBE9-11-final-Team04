package com.team04.domain.idea.dto.request;

import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.IdeaDraft;
import com.team04.domain.idea.entity.RewardType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** 아이디어와 3단계 마일스톤을 등록하기 위한 요청 DTO입니다. */
public record CreateIdeaRequest(
        @NotBlank @Size(max = 100) String title,
        @NotNull IdeaCategory category,
        @NotBlank @Size(max = 200) String oneLineIntro,
        @NotBlank @Size(max = 5000) String problemDefinition,
        @NotBlank @Size(max = 5000) String solution,
        @NotBlank @Size(max = 5000) String goal,
        @NotBlank @Size(max = 5000) String targetCustomer,
        @NotBlank @Size(max = 5000) String competitor,
        @NotBlank @Size(max = 5000) String teamIntro,
        @NotNull @Min(1_000_000) @Max(100_000_000) Long goalAmount,
        @NotNull @Future LocalDateTime fundingStartAt,
        @NotNull @Future LocalDateTime fundingEndAt,
        @NotNull RewardType rewardType,
        @NotEmpty @Size(min = 3, max = 3) List<@Valid @NotNull CreateMilestoneRequest> milestones
) {

    /** 펀딩 종료일이 시작일보다 이후인지 검증합니다. */
    @AssertTrue(message = "펀딩 종료일은 시작일 이후여야 합니다")
    public boolean isFundingEndAfterStart() {
        if (fundingStartAt == null || fundingEndAt == null) {
            return true;
        }
        return fundingEndAt.isAfter(fundingStartAt);
    }

    /** 펀딩 기간이 최소 2주에서 최대 8주 사이인지 검증합니다. */
    @AssertTrue(message = "펀딩 기간은 최소 2주, 최대 8주여야 합니다")
    public boolean isFundingPeriodInRange() {
        if (fundingStartAt == null || fundingEndAt == null || !fundingEndAt.isAfter(fundingStartAt)) {
            return true;
        }
        long days = ChronoUnit.DAYS.between(fundingStartAt, fundingEndAt);
        return days >= 14 && days <= 56;
    }
}
