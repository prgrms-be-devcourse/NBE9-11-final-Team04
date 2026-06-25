package com.team04.domain.settlement.entity;

import com.team04.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreSettlementTest {

    private PreSettlement createPreSettlement() {
        return PreSettlement.builder()
                .ideaId(1L)
                .amount(10000L)
                .build();
    }

    @Test
    @DisplayName("COMPLETED 상태에서 완료 콜백 재수신 시 멱등 처리")
    void complete_alreadyCompleted_noOp() {
        PreSettlement preSettlement = createPreSettlement();
        preSettlement.complete();

        preSettlement.complete();

        assertThat(preSettlement.getStatus()).isEqualTo(PreSettlementStatus.COMPLETED);
    }

    @Test
    @DisplayName("FAILED 상태에서 실패 콜백 재수신 시 멱등 처리")
    void fail_alreadyFailed_noOp() {
        PreSettlement preSettlement = createPreSettlement();
        preSettlement.fail();

        preSettlement.fail();

        assertThat(preSettlement.getStatus()).isEqualTo(PreSettlementStatus.FAILED);
    }

    @Test
    @DisplayName("FAILED 상태에서 완료 처리 시 예외 발생")
    void complete_failed_throwsException() {
        PreSettlement preSettlement = createPreSettlement();
        preSettlement.fail();

        assertThatThrownBy(preSettlement::complete)
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("FAILED 상태에서 REQUESTED로 재처리 대기 전환 성공")
    void retry_failed_success() {
        PreSettlement preSettlement = createPreSettlement();
        preSettlement.fail();

        preSettlement.retry();

        assertThat(preSettlement.getStatus()).isEqualTo(PreSettlementStatus.REQUESTED);
    }
}
