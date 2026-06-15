package com.team04.infra.external.government;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.infra.external.government.dto.response.BusinessVerificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessVerificationClient {

    private final WebClient webClient;

    @Value("${government.api.key}")
    private String apiKey;

    private static final String BASE_URL = "https://api.odcloud.kr/api/nts-businessman/v1";

    public boolean verify(String businessNumber) {
        Map<String, Object> requestBody = Map.of(
                "b_no", List.of(businessNumber)
        );

        BusinessVerificationResponse response = webClient.post()
                .uri(BASE_URL + "/status?serviceKey=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is5xxServerError(), res ->
                        res.bodyToMono(String.class)
                                .doOnNext(body -> log.error("국세청 API 에러: {}", body))
                                .then(Mono.error(new RuntimeException("국세청 API 500")))
                )
                .bodyToMono(BusinessVerificationResponse.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1))
                        .filter(throwable -> !(throwable instanceof WebClientResponseException.InternalServerError))
                )
                .onErrorMap(throwable -> new CustomException(ErrorCode.BUSINESS_VERIFICATION_UNAVAILABLE))
                .block();

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            return false;
        }

        return "01".equals(response.getData().get(0).getBSttCd());
    }
}
