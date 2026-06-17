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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Profile("!local")
@RequiredArgsConstructor
public class BusinessVerifyClientImpl implements BusinessVerifyClient {

    private final RestClient restClient;

    @Value("${nts.api.service-key}")
    private String ntsServiceKey;

    private static final String NTS_BASE_URL = "https://api.odcloud.kr/api/nts-businessman/v1";
    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_SECONDS = 1;

    @Override
    public boolean verify(String businessNumber, String representativeName, String startDate) {
        NtsValidateRequest body = new NtsValidateRequest(
                List.of(new NtsValidateRequest.Business(businessNumber, startDate, representativeName))
        );

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
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

            } catch (CustomException e) {
                throw e;
            } catch (Exception e) {
                log.warn("국세청 API 호출 실패 ({}/{}): {}", attempt, MAX_RETRY, e.getMessage());
                if (attempt == MAX_RETRY) {
                    throw new CustomException(ErrorCode.BUSINESS_VERIFICATION_UNAVAILABLE);
                }
                try {
                    TimeUnit.SECONDS.sleep(RETRY_DELAY_SECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new CustomException(ErrorCode.BUSINESS_VERIFICATION_UNAVAILABLE);
                }
            }
        }
        throw new CustomException(ErrorCode.BUSINESS_VERIFICATION_UNAVAILABLE);
    }
}
