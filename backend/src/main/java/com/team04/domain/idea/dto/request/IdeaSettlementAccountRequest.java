package com.team04.domain.idea.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 아이디어 제안자의 정산 및 환불 계좌 등록/수정 요청 DTO입니다. */
public record IdeaSettlementAccountRequest(
        @NotBlank @Size(max = 50) String bankName,
        @NotBlank @Size(max = 30) @Pattern(regexp = "^[0-9-]+$") String accountNumber,
        @NotBlank @Size(max = 50) String accountHolderName
) {
}
