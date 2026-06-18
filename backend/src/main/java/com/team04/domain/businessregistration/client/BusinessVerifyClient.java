package com.team04.domain.businessregistration.client;

import com.team04.global.exception.CustomException;

public interface BusinessVerifyClient {
    /**
     * @return true: 검증 성공, false: 검증 실패
     * @throws CustomException BUSINESS_VERIFICATION_UNAVAILABLE: API 장애 (재시도 3회 후)
     */
    boolean verify(String businessNumber, String representativeName, String startDate);
}
