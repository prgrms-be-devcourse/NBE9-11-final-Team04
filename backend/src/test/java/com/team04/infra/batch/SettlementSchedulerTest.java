package com.team04.infra.batch;

import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.milestone.service.MilestoneService;
import com.team04.domain.settlement.service.RefundService;
import com.team04.domain.settlement.service.SettlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private MilestoneService milestoneService;

    @Mock
    private SettlementService settlementService;

    @Mock
    private RefundService refundService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("목표 미달성 프로젝트 환불 장부 + 환불 레코드 생성 성공")
    void processClosedFundings_failedFunding_success() {
        // given
        given(ideaService.getSuccessfulFundingIdeaIds()).willReturn(List.of());
        given(ideaService.getFailedFundingIdeaIds()).willReturn(List.of(1L, 2L));
        // Consumer<TransactionStatus>를 실제로 실행시켜 내부 로직 검증
        doAnswer(invocation -> {
            ((java.util.function.Consumer<org.springframework.transaction.TransactionStatus>)
                    invocation.getArgument(0)).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        settlementScheduler.processClosedFundings();

        // then
        verify(settlementService, times(2)).createGoalNotMetRefundSettlement(anyLong());
        verify(refundService, times(2)).createGoalNotMetRefunds(anyLong());
    }

    @Test
    @DisplayName("목표 미달성 프로젝트 없으면 환불 처리 안함")
    void processClosedFundings_noClosedIdeas() {
        // given
        given(ideaService.getSuccessfulFundingIdeaIds()).willReturn(List.of());
        given(ideaService.getFailedFundingIdeaIds()).willReturn(List.of());

        // when
        settlementScheduler.processClosedFundings();

        // then
        verify(settlementService, never()).createGoalNotMetRefundSettlement(anyLong());
        verify(refundService, never()).createGoalNotMetRefunds(anyLong());
    }

    @Test
    @DisplayName("환불 처리 실패해도 다음 프로젝트 처리 계속됨")
    void processClosedFundings_continueOnError() {
        // given
        given(ideaService.getSuccessfulFundingIdeaIds()).willReturn(List.of());
        given(ideaService.getFailedFundingIdeaIds()).willReturn(List.of(1L, 2L));
        doThrow(new RuntimeException("실패")).when(transactionTemplate).executeWithoutResult(any());

        // when
        settlementScheduler.processClosedFundings();

        // then
        verify(transactionTemplate, times(2)).executeWithoutResult(any());
    }

    @Test
    @DisplayName("마감 후 목표 달성 프로젝트는 1단계 마일스톤 시작")
    void processClosedFundings_successfulFunding_startFirstMilestone() {
        // given
        given(ideaService.getSuccessfulFundingIdeaIds()).willReturn(List.of(1L, 2L));
        given(ideaService.getFailedFundingIdeaIds()).willReturn(List.of());
        doAnswer(invocation -> {
            ((java.util.function.Consumer<org.springframework.transaction.TransactionStatus>)
                    invocation.getArgument(0)).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        settlementScheduler.processClosedFundings();

        // then
        verify(ideaService).startFundingIfOpen(1L);
        verify(ideaService).startFundingIfOpen(2L);
        verify(milestoneService).startFirstMilestone(1L);
        verify(milestoneService).startFirstMilestone(2L);
        verify(settlementService, never()).createGoalNotMetRefundSettlement(anyLong());
        verify(refundService, never()).createGoalNotMetRefunds(anyLong());
    }
}
