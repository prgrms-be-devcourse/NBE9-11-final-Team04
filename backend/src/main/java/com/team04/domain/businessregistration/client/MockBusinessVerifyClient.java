package com.team04.domain.businessregistration.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
public class MockBusinessVerifyClient implements BusinessVerifyClient {

    private static final String VALID_BUSINESS_NUMBER = "1234567890";

    @Override
    public boolean verify(String businessNumber, String representativeName, String startDate) {
        log.info("[MockBusinessVerifyClient] 검증 요청: businessNumber={}", businessNumber);

        boolean result = VALID_BUSINESS_NUMBER.equals(businessNumber);
        log.info("[MockBusinessVerifyClient] 검증 결과: {}", result ? "성공" : "실패");
        return result;
    }
}
