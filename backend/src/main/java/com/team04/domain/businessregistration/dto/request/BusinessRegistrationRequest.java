package com.team04.domain.businessregistration.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BusinessRegistrationRequest(
        @NotBlank
        String businessNumber,
        @NotBlank
        String representativeName,
        @NotBlank @Pattern(
                regexp = "^\\d{8}$",
                message = "개업일자는 YYYYMMDD 형식의 8자리 숫자여야 합니다.")
        String openDate
) {
}
