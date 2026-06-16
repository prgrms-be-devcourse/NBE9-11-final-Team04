package com.team04.domain.expert.client;

import com.team04.domain.expert.dto.request.ExpertVerifyRequest;
import com.team04.global.exception.CustomException;

public interface ExternalVerifyClient {
    /**
     * @return true: 검증 성공, false: 검증 실패
     * @throws CustomException EXTERNAL_API_FAILURE: API 장애 (재시도 3회 후)
     */
    boolean verify(ExpertVerifyRequest request);
}
