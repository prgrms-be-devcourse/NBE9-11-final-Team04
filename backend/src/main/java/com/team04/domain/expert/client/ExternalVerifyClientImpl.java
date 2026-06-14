package com.team04.domain.expert.client;

import com.team04.domain.expert.client.nts.dto.NtsValidateRequest;
import com.team04.domain.expert.client.nts.dto.NtsValidateResponse;
import com.team04.domain.expert.dto.request.ExpertVerifyRequest;
import com.team04.domain.expert.entity.QualificationType;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalVerifyClientImpl implements ExternalVerifyClient{

    private final RestClient restClient;

    @Value("${nts.api.service-key}")
    private String ntsServiceKey;

    private static final String NTS_BASE_URL = "https://api.odcloud.kr/api/nts-businessman/v1";
    private static final int MAX_RETRY = 3;

    @Override
    public boolean verify(ExpertVerifyRequest request) {
        if (request.qualificationType() == QualificationType.BUSINESS_REGISTRATION) {
            return verifyBusinessRegistration(request);
        }
        // NATIONAL_QUALIFICATION은 관리자 수동 검토 → 항상 보류 처리
        return false;
    }

    private boolean verifyBusinessRegistration(ExpertVerifyRequest request) {
        validateBusinessRequest(request);

        NtsValidateRequest body = new NtsValidateRequest(
                List.of(new NtsValidateRequest.Business(
                        request.qualificationNumber(),
                        request.startDate(),
                        request.representativeName()
                ))
        );

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                NtsValidateResponse response = restClient.post()
                        .uri(NTS_BASE_URL + "/validate?serviceKey={key}&returnType=JSON", ntsServiceKey)
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
                    throw new CustomException(ErrorCode.EXTERNAL_API_FAILURE);
                }
            }
        }
        throw new CustomException(ErrorCode.EXTERNAL_API_FAILURE);
    }

    /* 사업자번호 검증에 필요한 필수 데이터 존재 확인 */
    private void validateBusinessRequest(ExpertVerifyRequest request) {
        if (request.startDate() == null || request.startDate().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (request.representativeName() == null || request.representativeName().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}

