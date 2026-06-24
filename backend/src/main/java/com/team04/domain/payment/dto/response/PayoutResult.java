package com.team04.domain.payment.dto.response;

/** 지급대행 API 호출 결과 */
public record PayoutResult(
        boolean success,
        String payoutId,
        String failureMessage
) {
    public static PayoutResult success(String payoutId) {
        return new PayoutResult(true, payoutId, null);
    }

    public static PayoutResult failure(String message) {
        return new PayoutResult(false, null, message);
    }

    public static PayoutResult skipped(String reason) {
        return new PayoutResult(true, "skipped:" + reason, null);
    }
}
