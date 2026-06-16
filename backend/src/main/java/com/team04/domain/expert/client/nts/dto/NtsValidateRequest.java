package com.team04.domain.expert.client.nts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NtsValidateRequest(
        List<Business> businesses
) {
    public record Business(
            @JsonProperty("b_no") String bNo,           // 사업자번호
            @JsonProperty("start_dt") String startDt,   // 창업일자
            @JsonProperty("p_nm") String pNm            // 대표자 성명
    ) {}
}