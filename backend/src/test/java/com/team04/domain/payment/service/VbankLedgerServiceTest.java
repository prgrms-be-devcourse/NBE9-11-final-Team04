package com.team04.domain.payment.service;

import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.payment.dto.response.VbankLedgerResponse;
import com.team04.domain.payment.entity.VbankLedger;
import com.team04.domain.payment.entity.VbankLedgerDirection;
import com.team04.domain.payment.entity.VbankLedgerType;
import com.team04.domain.payment.repository.VbankLedgerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VbankLedgerServiceTest {

    @InjectMocks
    private VbankLedgerService vbankLedgerService;

    @Mock
    private VbankLedgerRepository vbankLedgerRepository;
    @Mock
    private IdeaService ideaService;
    @Mock
    private IdeaRepository ideaRepository;
    @Mock
    private FundingRepository fundingRepository;

    @Test
    @DisplayName("입금 장부는 기존 잔액에 금액을 더한다")
    void recordIn_addsBalance() {
        given(vbankLedgerRepository.findByIdempotencyKey("key-1")).willReturn(Optional.empty());
        given(ideaRepository.findByIdForUpdate(1L)).willReturn(Optional.of(mock(Idea.class)));
        given(vbankLedgerRepository.findTopByIdeaIdAndAffectsBalanceOrderByIdDesc(1L, true))
                .willReturn(Optional.of(ledger(1L, 100_000L)));
        given(vbankLedgerRepository.save(any(VbankLedger.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        VbankLedgerResponse response = vbankLedgerService.recordIn(
                1L,
                VbankLedgerType.FUNDING_PAID,
                50_000L,
                "key-1",
                "Payment",
                10L,
                "후원금 입금"
        );

        assertThat(response.balanceAfter()).isEqualTo(150_000L);
        assertThat(response.direction()).isEqualTo(VbankLedgerDirection.IN);
        assertThat(response.affectsBalance()).isTrue();
    }

    @Test
    @DisplayName("잔액에 영향을 주지 않는 공개용 출금 기록은 잔액을 유지한다")
    void recordDisclosureOut_keepsBalance() {
        given(vbankLedgerRepository.findByIdempotencyKey("usage-1")).willReturn(Optional.empty());
        given(ideaRepository.findByIdForUpdate(1L)).willReturn(Optional.of(mock(Idea.class)));
        given(vbankLedgerRepository.findTopByIdeaIdAndAffectsBalanceOrderByIdDesc(1L, true))
                .willReturn(Optional.of(ledger(1L, 100_000L)));
        given(vbankLedgerRepository.save(any(VbankLedger.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        VbankLedgerResponse response = vbankLedgerService.recordDisclosureOut(
                1L,
                VbankLedgerType.FUND_USAGE_RECORDED,
                30_000L,
                "usage-1",
                "FundUsage",
                20L,
                "재료비"
        );

        ArgumentCaptor<VbankLedger> captor = ArgumentCaptor.forClass(VbankLedger.class);
        verify(vbankLedgerRepository).save(captor.capture());
        assertThat(response.balanceAfter()).isEqualTo(100_000L);
        assertThat(response.direction()).isEqualTo(VbankLedgerDirection.OUT);
        assertThat(captor.getValue().getAffectsBalance()).isFalse();
    }

    private VbankLedger ledger(Long ideaId, Long balanceAfter) {
        return VbankLedger.create(
                ideaId,
                VbankLedgerType.FUNDING_PAID,
                VbankLedgerDirection.IN,
                balanceAfter,
                balanceAfter,
                true,
                "existing-" + balanceAfter,
                "Payment",
                1L,
                null
        );
    }
}
