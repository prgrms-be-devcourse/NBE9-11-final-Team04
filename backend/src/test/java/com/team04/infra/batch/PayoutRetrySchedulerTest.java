package com.team04.infra.batch;

import com.team04.domain.payment.service.SettlementPaymentService;
import com.team04.domain.settlement.entity.PreSettlement;
import com.team04.domain.settlement.entity.Settlement;
import com.team04.domain.settlement.entity.SettlementStatus;
import com.team04.domain.settlement.entity.SettlementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PayoutRetrySchedulerTest {

    @InjectMocks
    private PayoutRetryScheduler payoutRetryScheduler;

    @Mock
    private SettlementPaymentService settlementPaymentService;

    @Test
    @DisplayName("FAILED 선정산과 정산 지급 건을 재처리한다")
    void retryFailedPayouts_success() {
        PreSettlement preSettlement = PreSettlement.builder()
                .ideaId(1L)
                .amount(10_000L)
                .build();
        setField(preSettlement, "id", 1L);

        Settlement finalSettlement = Settlement.builder()
                .ideaId(1L)
                .type(SettlementType.FINAL)
                .totalAmount(100_000L)
                .platformFee(1_000L)
                .payoutAmount(99_000L)
                .idempotencyKey("idea-1-FINAL")
                .build();
        setField(finalSettlement, "id", 2L);

        Settlement depositSettlement = Settlement.builder()
                .ideaId(1L)
                .type(SettlementType.FINAL)
                .totalAmount(30_000L)
                .platformFee(0L)
                .payoutAmount(30_000L)
                .idempotencyKey("idea-1-DEPOSIT-COMPLETED")
                .build();
        setField(depositSettlement, "id", 3L);

        given(settlementPaymentService.findFailedPreSettlements()).willReturn(List.of(preSettlement));
        given(settlementPaymentService.findFailedSettlements()).willReturn(List.of(finalSettlement, depositSettlement));

        payoutRetryScheduler.retryFailedPayouts();

        verify(settlementPaymentService).retryPreSettlementPayout(1L);
        verify(settlementPaymentService).retrySettlementPayout(2L, SettlementStatus.COMPLETED);
        verify(settlementPaymentService).retrySettlementPayout(3L, SettlementStatus.DEPOSIT_REFUNDED);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
