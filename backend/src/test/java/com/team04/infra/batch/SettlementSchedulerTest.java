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

import java.util.List;

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

    @Test
    @DisplayName("목표 미달성 프로젝트 환불 장부 + 환불 레코드 생성 성공")
    void processFailedFundingRefunds_success() {
        // given
        given(ideaService.getFailedFundingIdeaIds()).willReturn(List.of(1L, 2L));

        // when
        settlementScheduler.processFailedFundingRefunds();

        // then
        verify(settlementService, times(2)).createGoalNotMetRefundSettlement(anyLong());
        verify(refundService, times(2)).createGoalNotMetRefunds(anyLong());
    }

    @Test
    @DisplayName("목표 미달성 프로젝트 없으면 환불 처리 안함")
    void processFailedFundingRefunds_noFailedIdeas() {
        // given
        given(ideaService.getFailedFundingIdeaIds()).willReturn(List.of());

        // when
        settlementScheduler.processFailedFundingRefunds();

        // then
        verify(settlementService, never()).createGoalNotMetRefundSettlement(anyLong());
        verify(refundService, never()).createGoalNotMetRefunds(anyLong());
    }

    @Test
    @DisplayName("환불 처리 실패해도 다음 프로젝트 처리 계속됨")
    void processFailedFundingRefunds_continueOnError() {
        // given
        given(ideaService.getFailedFundingIdeaIds()).willReturn(List.of(1L, 2L));
        doThrow(new RuntimeException("실패")).when(settlementService).createGoalNotMetRefundSettlement(1L);

        // when
        settlementScheduler.processFailedFundingRefunds();

        // then
        verify(settlementService, times(2)).createGoalNotMetRefundSettlement(anyLong());
    }
}