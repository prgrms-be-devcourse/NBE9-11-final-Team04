package com.team04.domain.payment.dto.response;

/** 프론트 시연용 결제 설정 */
public record PaymentConfigResponse(
        boolean demoMode,
        String clientKey,
        String gatewayType
) {
}
