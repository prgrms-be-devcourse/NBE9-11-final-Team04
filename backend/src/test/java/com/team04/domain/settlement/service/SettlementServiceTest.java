package com.team04.domain.settlement.service;

import com.team04.domain.funding.service.FundingService;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.settlement.entity.PreSettlementStatus;
import com.team04.domain.settlement.entity.Settlement;
import com.team04.domain.settlement.repository.PreSettlementRepository;
import com.team04.domain.settlement.repository.SettlementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private IdeaService ideaService;

    @Mock
    private PreSettlementRepository preSettlementRepository;

    @Mock
    private RefundService refundService;

    @Mock
    private FundingService fundingService;

    @InjectMocks
    private SettlementService settlementService;

    @Test
    @DisplayName("관리자 강제 환불 시 정산 장부에 사유를 기록한다")
    void forceRefund_recordsReason() {
        String reason = "운영 정책 위반으로 강제 환불";
        given(settlementRepository.findByIdempotencyKey("idea-1-REFUND-CANCELLED"))
                .willReturn(Optional.empty(), Optional.empty());
        given(settlementRepository.findByIdempotencyKey("idea-1-DEPOSIT-FORFEITED"))
                .willReturn(Optional.empty(), Optional.empty());
        given(ideaService.getIdea(1L)).willReturn(sampleIdea());
        given(preSettlementRepository.sumAmountByIdeaIdAndStatus(1L, PreSettlementStatus.COMPLETED))
                .willReturn(0L);
        given(settlementRepository.save(any(Settlement.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        settlementService.forceRefund(1L, reason);

        ArgumentCaptor<Settlement> settlementCaptor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository, times(2)).save(settlementCaptor.capture());
        assertThat(settlementCaptor.getAllValues())
                .extracting(Settlement::getMemo)
                .containsExactly(reason, reason);
        verify(refundService).createCancelRefunds(1L, false);
    }

    private static IdeaResponse sampleIdea() {
        LocalDateTime now = LocalDateTime.now();
        return new IdeaResponse(
                1L,
                10L,
                "title",
                "TECH",
                "intro",
                "problem",
                "solution",
                "goal",
                "target",
                "competitor",
                "team",
                1_000_000L,
                100_000L,
                200_000L,
                1,
                now,
                now.plusDays(7),
                "REWARD_POINT",
                null,
                List.of(),
                "IN_PROGRESS",
                null,
                null,
                "NONE",
                now,
                now,
                List.of()
        );
    }
}
