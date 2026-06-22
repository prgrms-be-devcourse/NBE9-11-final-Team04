package com.team04.domain.expert.client;

import com.team04.domain.expert.dto.request.ExpertVerifyRequest;
import com.team04.global.exception.CustomException;

public interface ExternalVerifyClient {
    /**
     * @param request 검증 요청 정보
     * @param retry true: 실패 시 최대 3회 재시도 (신규 검증용), false: 1회만 시도 (스케줄러용)
     * @return true: 검증 성공, false: 검증 실패
     */
    boolean verify(ExpertVerifyRequest request, boolean retry);
}
