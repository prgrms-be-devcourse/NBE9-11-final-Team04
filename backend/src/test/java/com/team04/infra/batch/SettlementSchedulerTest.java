package com.team04.infra.batch;

import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.settlement.service.RefundService;
import com.team04.domain.settlement.service.SettlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementSchedulerTest {

    @InjectMocks
    private SettlementScheduler settlementScheduler;

    @Mock
    private IdeaService ideaService;

    @Mock
    private SettlementService settlementService;

    @Mock
    private RefundService refundService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("목표 미달성 프로젝트 환불 장부 + 환불 레코드 생성 후 PG 실행")
    void processFailedFundingRefunds_success() {
        given(ideaService.getFailedFundingIdeaIds()).willReturn(List.of(1L, 2L));
        given(refundService.createGoalNotMetRefunds(anyLong())).willReturn(List.of(10L, 11L));
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());

        settlementScheduler.processFailedFundingRefunds();

        verify(settlementService, times(2)).createGoalNotMetRefundSettlement(anyLong());
        verify(refundService, times(2)).createGoalNotMetRefunds(anyLong());
        verify(refundService, times(4)).executeRefund(anyLong());
        verify(refundService, times(2)).tryFinalizeRefundSettlement(anyLong());
    }

    @Test
    @DisplayName("목표 미달성 프로젝트 없으면 환불 처리 안함")
    void processFailedFundingRefunds_noFailedIdeas() {
        given(ideaService.getFailedFundingIdeaIds()).willReturn(List.of());

        settlementScheduler.processFailedFundingRefunds();

        verify(settlementService, never()).createGoalNotMetRefundSettlement(anyLong());
        verify(refundService, never()).createGoalNotMetRefunds(anyLong());
    }

    @Test
    @DisplayName("환불 처리 실패해도 다음 프로젝트 처리 계속됨")
    void processFailedFundingRefunds_continueOnError() {
        given(ideaService.getFailedFundingIdeaIds()).willReturn(List.of(1L, 2L));
        doThrow(new RuntimeException("실패")).when(transactionTemplate).execute(any());

        settlementScheduler.processFailedFundingRefunds();

        verify(transactionTemplate, times(2)).execute(any());
    }
}
