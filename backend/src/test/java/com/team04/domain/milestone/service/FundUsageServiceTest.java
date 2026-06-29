package com.team04.domain.milestone.service;

import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.milestone.dto.request.FundUsageRequest;
import com.team04.domain.milestone.dto.response.FundUsageResponse;
import com.team04.domain.milestone.entity.FundUsage;
import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.entity.MilestoneStatus;
import com.team04.domain.milestone.repository.FundUsageRepository;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.domain.payment.entity.VbankLedgerType;
import com.team04.domain.payment.service.VbankLedgerService;
import com.team04.domain.settlement.entity.PreSettlementStatus;
import com.team04.domain.settlement.repository.PreSettlementRepository;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FundUsageServiceTest {

    @Mock
    private FundUsageRepository fundUsageRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private PreSettlementRepository preSettlementRepository;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private IdeaService ideaService;

    @Mock
    private IdeaRepository ideaRepository;

    @Mock
    private VbankLedgerService vbankLedgerService;

    @InjectMocks
    private FundUsageService fundUsageService;

    @Test
    @DisplayName("제안자는 지급받은 선정산 한도 안에서 자금 사용 내역을 등록할 수 있다")
    void addFundUsage_success() {
        Idea lockedIdea = lockedIdea(99L, IdeaStatus.IN_PROGRESS, LocalDateTime.now().minusDays(10));
        FundUsageRequest request = new FundUsageRequest("서버 비용", 100_000L, LocalDate.now());
        given(ideaRepository.findByIdForUpdate(10L)).willReturn(Optional.of(lockedIdea));
        given(milestoneRepository.findByIdeaIdAndStatus(10L, MilestoneStatus.IN_PROGRESS))
                .willReturn(Optional.of(mock(Milestone.class)));
        given(preSettlementRepository.sumAmountByIdeaIdAndStatus(10L, PreSettlementStatus.COMPLETED))
                .willReturn(200_000L);
        given(fundUsageRepository.sumAmountByIdeaId(10L)).willReturn(50_000L);
        given(fundUsageRepository.save(any(FundUsage.class)))
                .willAnswer(invocation -> {
                    FundUsage saved = invocation.getArgument(0);
                    setField(saved, "id", 1L);
                    return saved;
                });

        FundUsageResponse response = fundUsageService.addFundUsage(10L, request, 99L);

        assertThat(response.fundUsageId()).isEqualTo(1L);
        assertThat(response.ideaId()).isEqualTo(10L);
        assertThat(response.amount()).isEqualTo(100_000L);
        verify(vbankLedgerService).recordDisclosureOut(
                10L,
                VbankLedgerType.FUND_USAGE_RECORDED,
                100_000L,
                "fund-usage-1",
                "FundUsage",
                1L,
                "서버 비용"
        );
    }

    @Test
    @DisplayName("실제 지급받은 선정산 금액을 초과하는 지출 등록은 거부한다")
    void addFundUsage_exceedsReceivedAmount_throwsException() {
        Idea lockedIdea = lockedIdea(99L, IdeaStatus.IN_PROGRESS, LocalDateTime.now().minusDays(10));
        FundUsageRequest request = new FundUsageRequest("서버 비용", 100_000L, LocalDate.now());
        given(ideaRepository.findByIdForUpdate(10L)).willReturn(Optional.of(lockedIdea));
        given(milestoneRepository.findByIdeaIdAndStatus(10L, MilestoneStatus.IN_PROGRESS))
                .willReturn(Optional.of(mock(Milestone.class)));
        given(preSettlementRepository.sumAmountByIdeaIdAndStatus(10L, PreSettlementStatus.COMPLETED))
                .willReturn(120_000L);
        given(fundUsageRepository.sumAmountByIdeaId(10L)).willReturn(50_000L);

        assertThatThrownBy(() -> fundUsageService.addFundUsage(10L, request, 99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUND_USAGE_EXCEEDS_RECEIVED);
        verify(fundUsageRepository, never()).save(any());
        verify(vbankLedgerService, never()).recordDisclosureOut(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("펀딩 시작일 이전 지출 등록은 거부한다")
    void addFundUsage_beforeFundingStart_throwsException() {
        Idea lockedIdea = lockedIdea(99L, IdeaStatus.IN_PROGRESS, LocalDateTime.now());
        FundUsageRequest request = new FundUsageRequest("서버 비용", 100_000L, LocalDate.now().minusDays(1));
        given(ideaRepository.findByIdForUpdate(10L)).willReturn(Optional.of(lockedIdea));
        given(milestoneRepository.findByIdeaIdAndStatus(10L, MilestoneStatus.IN_PROGRESS))
                .willReturn(Optional.of(mock(Milestone.class)));

        assertThatThrownBy(() -> fundUsageService.addFundUsage(10L, request, 99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUND_USAGE_INVALID_DATE);
        verify(fundUsageRepository, never()).save(any());
    }

    @Test
    @DisplayName("결제 성공 후원자는 자금 사용 내역을 조회할 수 있다")
    void getFundUsages_paidSponsor_success() {
        FundUsage fundUsage = FundUsage.builder()
                .ideaId(10L)
                .itemName("서버 비용")
                .amount(100_000L)
                .usedAt(LocalDate.now())
                .build();
        setField(fundUsage, "id", 1L);
        given(ideaService.getIdea(10L)).willReturn(sampleIdea(99L));
        given(fundingRepository.existsPaidSponsorByIdeaIdAndSponsorId(10L, 20L)).willReturn(true);
        given(fundUsageRepository.findByIdeaIdOrderByUsedAtDesc(10L)).willReturn(List.of(fundUsage));

        List<FundUsageResponse> responses = fundUsageService.getFundUsages(10L, 20L, Role.USER);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).fundUsageId()).isEqualTo(1L);
        verify(fundingRepository).existsPaidSponsorByIdeaIdAndSponsorId(10L, 20L);
    }

    @Test
    @DisplayName("제안자도 후원 여부와 관계없이 자금 사용 내역을 조회할 수 있다")
    void getFundUsages_owner_success() {
        given(ideaService.getIdea(10L)).willReturn(sampleIdea(99L));
        given(fundUsageRepository.findByIdeaIdOrderByUsedAtDesc(10L)).willReturn(List.of());

        List<FundUsageResponse> responses = fundUsageService.getFundUsages(10L, 99L, Role.USER);

        assertThat(responses).isEmpty();
        verify(fundingRepository, never()).existsPaidSponsorByIdeaIdAndSponsorId(10L, 99L);
    }

    private static Idea lockedIdea(Long userId, IdeaStatus status, LocalDateTime fundingStartAt) {
        Idea idea = mock(Idea.class);
        given(idea.getUserId()).willReturn(userId);
        given(idea.getStatus()).willReturn(status);
        given(idea.getFundingStartAt()).willReturn(fundingStartAt);
        return idea;
    }

    private static IdeaResponse sampleIdea(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return new IdeaResponse(
                10L,
                userId,
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
                now.minusDays(10),
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
