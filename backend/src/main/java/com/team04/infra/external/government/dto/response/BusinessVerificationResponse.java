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
        @JsonProperty("valid")
        private String valid;  // 01: 일치, 02: 불일치
    }
}