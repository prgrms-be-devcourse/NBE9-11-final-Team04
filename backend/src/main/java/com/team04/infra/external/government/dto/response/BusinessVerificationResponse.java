package com.team04.infra.external.government.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BusinessVerificationResponse {

    private List<Data> data;

    @Getter
    @NoArgsConstructor
    public static class Data {
        @JsonProperty("b_stt_cd")
        private String bSttCd;  // 01: 계속사업자, 02: 휴업, 03: 폐업
    }
}