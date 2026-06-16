package com.team04.domain.expert.client;

import com.team04.domain.expert.dto.request.ExpertVerifyRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
public class MockVerifyClient implements ExternalVerifyClient {

    private static final String VALID_BUSINESS_NUMBER = "1234567890";

    @Override
    public boolean verify(ExpertVerifyRequest request) {
        log.info("[MockVerifyClient] 검증 요청: type={}, number={}",
                request.qualificationType(), request.qualificationNumber());

        boolean result = VALID_BUSINESS_NUMBER.equals(request.qualificationNumber());
        log.info("[MockVerifyClient] 검증 결과: {}", result ? "성공" : "실패");
        return result;
    }
}