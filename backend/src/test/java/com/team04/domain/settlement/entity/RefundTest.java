package com.team04.domain.settlement.entity;

import com.team04.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundTest {

    private Refund createRefund() {
        return Refund.builder()
                .paymentId(1L)
                .sponsorId(10L)
                .amount(10000L)
                .reason(RefundReason.CANCELLED)
                .build();
    }

    @Test
    @DisplayName("FAILED 상태에서 실패 콜백 재수신 시 멱등 처리")
    void fail_alreadyFailed_noOp() {
        Refund refund = createRefund();
        refund.fail();

        refund.fail();

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
    }

    @Test
    @DisplayName("COMPLETED 상태에서 완료 콜백 재수신 시 멱등 처리")
    void complete_alreadyCompleted_noOp() {
        Refund refund = createRefund();
        refund.complete();

        refund.complete();

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
    }

    @Test
    @DisplayName("COMPLETED 상태에서 실패 처리 시 예외 발생")
    void fail_completed_throwsException() {
        Refund refund = createRefund();
        refund.complete();

        assertThatThrownBy(refund::fail)
                .isInstanceOf(CustomException.class);
    }
}
