package com.team04.idea.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** 아이디어 등록 시 함께 생성할 마일스톤 요청 정보를 담는 DTO입니다. */
public record CreateMilestoneRequest(
        @NotNull @Min(1) @Max(3) Integer step,
        @NotBlank @Size(max = 1000) String goal,
        @NotBlank @Size(max = 1000) String expectedResult,
        @NotNull @Future LocalDate expectedDate
) {
}
