package com.team04.domain.businessregistration.client;

import com.team04.domain.expert.client.nts.dto.NtsValidateRequest;
import com.team04.domain.expert.client.nts.dto.NtsValidateResponse;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
@Profile("!local")
@RequiredArgsConstructor
public class BusinessVerifyClientImpl implements BusinessVerifyClient {

    private final RestClient restClient;

    @Value("${nts.api.service-key}")
    private String ntsServiceKey;

    private static final String NTS_BASE_URL = "https://api.odcloud.kr/api/nts-businessman/v1";

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000), exclude = CustomException.class)
    @Override
    public boolean verify(String businessNumber, String representativeName, String startDate) {
        NtsValidateRequest body = new NtsValidateRequest(
                List.of(new NtsValidateRequest.Business(businessNumber, startDate, representativeName))
        );

        URI uri = UriComponentsBuilder
                .fromUriString(NTS_BASE_URL + "/validate")
                .queryParam("serviceKey", ntsServiceKey)
                .queryParam("returnType", "JSON")
                .build()
                .encode()
                .toUri();

        NtsValidateResponse response = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(NtsValidateResponse.class);

        if (response == null) {
            throw new IllegalStateException("국세청 API 응답 바디가 비어 있습니다.");
        }

        return response.isValid();
    }

    @Recover
    public boolean verifyFallback(Exception e, String businessNumber,
                                  String representativeName, String startDate) {
        log.error("국세청 API 3회 모두 실패: {}", e.getMessage());
        throw new CustomException(ErrorCode.BUSINESS_VERIFICATION_UNAVAILABLE);
    }

}
