package com.team04.domain.expert.client.nts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NtsValidateResponse(
        @JsonProperty("status_code") String statusCode,
        @JsonProperty("match_cnt") String matchCnt,         // 국세청 데이터와 정확히 일치하는 데이터 건수
        @JsonProperty("request_cnt") String requestCnt,     // 조회를 요청한 건수
        List<Data> data
) {
    public record Data(
            @JsonProperty("b_no") String bNo,
            @JsonProperty("valid") String valid,
            @JsonProperty("valid_msg") String validMsg

    ) {}

    public boolean isValid() {
        return data != null && !data.isEmpty() && "01".equals(data.get(0).valid);   // 01 : 진위 확인 성공 | 02 : 진위 확인 실패
    }
}
