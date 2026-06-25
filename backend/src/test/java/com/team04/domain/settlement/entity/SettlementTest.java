package com.team04.domain.settlement.entity;

import com.team04.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementTest {

    private Settlement createSettlement() {
        return Settlement.builder()
                .ideaId(1L)
                .type(SettlementType.FINAL)
                .totalAmount(100000L)
                .platformFee(10000L)
                .payoutAmount(90000L)
                .idempotencyKey("idea-1-FINAL")
                .build();
    }

    @Test
    @DisplayName("PENDING 상태에서 COMPLETED로 전이 성공")
    void complete_success() {
        Settlement settlement = createSettlement();
        settlement.complete();
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
    }

    @Test
    @DisplayName("PENDING 상태에서 PARTIALLY_REFUNDED로 전이 성공")
    void partialRefund_success() {
        Settlement settlement = createSettlement();
        settlement.partialRefund();
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PARTIALLY_REFUNDED);
    }

    @Test
    @DisplayName("PENDING 상태에서 FORFEITED로 전이 성공")
    void forfeit_success() {
        Settlement settlement = createSettlement();
        settlement.forfeit();
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.FORFEITED);
    }

    @Test
    @DisplayName("PENDING 상태에서 REFUNDED로 전이 성공")
    void refund_success() {
        Settlement settlement = createSettlement();
        settlement.refund();
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.REFUNDED);
    }

    @Test
    @DisplayName("PENDING 상태에서 FAILED로 전이 성공")
    void fail_success() {
        Settlement settlement = createSettlement();
        settlement.fail();
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.FAILED);
    }

    @Test
    @DisplayName("FAILED 상태에서 PENDING으로 재처리 대기 전환 성공")
    void retryPayout_failed_success() {
        Settlement settlement = createSettlement();
        settlement.fail();

        settlement.retryPayout();

        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PENDING);
    @DisplayName("정산 장부 메모 기록 성공")
    void recordMemo_success() {
        Settlement settlement = createSettlement();

        settlement.recordMemo("관리자 강제 환불 사유");

        assertThat(settlement.getMemo()).isEqualTo("관리자 강제 환불 사유");
    }

    @Test
    @DisplayName("COMPLETED 상태에서 전이 시 예외 발생")
    void complete_alreadyCompleted() {
        Settlement settlement = createSettlement();
        settlement.complete();
        assertThatThrownBy(settlement::complete)
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("현재 상태에서 해당 상태로 전이할 수 없습니다");
    }

    @Test
    @DisplayName("금액 정합성 검증 실패 시 예외 발생 — platformFee + payoutAmount가 totalAmount 초과")
    void builder_invalidAmount() {
        assertThatThrownBy(() -> Settlement.builder()
                .ideaId(1L)
                .type(SettlementType.FINAL)
                .totalAmount(100000L)
                .platformFee(10000L)
                .payoutAmount(110000L)
                .idempotencyKey("idea-1-FINAL")
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
