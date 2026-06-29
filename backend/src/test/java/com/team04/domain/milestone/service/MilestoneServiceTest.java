package com.team04.domain.milestone.service;

import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.funding.service.FundingService;
import com.team04.domain.idea.dto.response.IdeaSummaryResponse;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.milestone.dto.request.CompletionReportRequest;
import com.team04.domain.milestone.dto.request.RejectReportRequest;
import com.team04.domain.milestone.dto.response.CompletionReportResponse;
import com.team04.domain.milestone.entity.CompletionReport;
import com.team04.domain.milestone.entity.CompletionReportStatus;
import com.team04.domain.milestone.entity.CompletionReportType;
import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.entity.MilestoneStatus;
import com.team04.domain.milestone.repository.CompletionReportRepository;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.domain.settlement.service.RefundService;
import com.team04.domain.settlement.service.SettlementService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.storage.MilestoneReportStorageClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MilestoneServiceTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private CompletionReportRepository completionReportRepository;

    @Mock
    private IdeaService ideaService;

    @Mock
    private SettlementService settlementService;

    @Mock
    private RefundService refundService;

    @Mock
    private FundingService fundingService;

    @Mock
    private MilestoneReportStorageClient milestoneReportStorageClient;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MilestoneService milestoneService;

    @Test
    @DisplayName("진행 중인 마일스톤에 완료 보고서를 제출한다")
    void submitCompletionReport_success() {
        Milestone milestone = milestone(1L, 1);
        milestone.start();
        milestone.markOverdue(LocalDateTime.now());
        given(milestoneRepository.findById(1L)).willReturn(Optional.of(milestone));
        given(completionReportRepository.findByMilestoneIdAndType(1L, CompletionReportType.COMPLETION))
                .willReturn(Optional.empty());
        given(completionReportRepository.save(any(CompletionReport.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        CompletionReportResponse response = milestoneService.submitCompletionReport(
                1L,
                new CompletionReportRequest("완료 보고서"),
                null
        );

        assertThat(response.status()).isEqualTo(CompletionReportStatus.SUBMITTED);
        assertThat(response.type()).isEqualTo(CompletionReportType.COMPLETION);
        assertThat(response.content()).isEqualTo("완료 보고서");
        assertThat(milestone.getOverdueAt()).isNull();
        verify(ideaService).validateNotSuspended(10L);
    }

    @Test
    @DisplayName("완료 보고서가 이미 있으면 중복 제출을 거부한다")
    void submitCompletionReport_duplicate_throwsException() {
        Milestone milestone = milestone(1L, 1);
        milestone.start();
        given(milestoneRepository.findById(1L)).willReturn(Optional.of(milestone));
        given(completionReportRepository.findByMilestoneIdAndType(1L, CompletionReportType.COMPLETION))
                .willReturn(Optional.of(report(1L, CompletionReportType.COMPLETION)));

        assertThatThrownBy(() -> milestoneService.submitCompletionReport(
                1L,
                new CompletionReportRequest("완료 보고서"),
                null
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MILESTONE_ALREADY_COMPLETED);
        verify(completionReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("반려된 보고서 이후 소명 보고서를 제출한다")
    void submitAppealReport_success() {
        Milestone milestone = milestone(1L, 1);
        milestone.start();
        milestone.markOverdue(LocalDateTime.now());
        CompletionReport rejectedReport = report(1L, CompletionReportType.COMPLETION);
        rejectedReport.reject("보완 필요");
        given(milestoneRepository.findById(1L)).willReturn(Optional.of(milestone));
        given(completionReportRepository.findTopByMilestoneIdOrderBySubmittedAtDesc(1L))
                .willReturn(Optional.of(rejectedReport));
        given(completionReportRepository.countByMilestoneIdAndType(1L, CompletionReportType.APPEAL))
                .willReturn(1L);
        given(completionReportRepository.save(any(CompletionReport.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        CompletionReportResponse response = milestoneService.submitAppealReport(
                1L,
                new CompletionReportRequest("소명 보고서"),
                null
        );

        assertThat(response.status()).isEqualTo(CompletionReportStatus.SUBMITTED);
        assertThat(response.type()).isEqualTo(CompletionReportType.APPEAL);
        assertThat(milestone.getOverdueAt()).isNull();
    }

    @Test
    @DisplayName("소명 보고서는 3회를 초과해서 제출할 수 없다")
    void submitAppealReport_limitExceeded_throwsException() {
        CompletionReport rejectedReport = report(1L, CompletionReportType.APPEAL);
        rejectedReport.reject("보완 필요");
        given(milestoneRepository.findById(1L)).willReturn(Optional.of(milestone(1L, 1)));
        given(completionReportRepository.findTopByMilestoneIdOrderBySubmittedAtDesc(1L))
                .willReturn(Optional.of(rejectedReport));
        given(completionReportRepository.countByMilestoneIdAndType(1L, CompletionReportType.APPEAL))
                .willReturn(3L);

        assertThatThrownBy(() -> milestoneService.submitAppealReport(
                1L,
                new CompletionReportRequest("소명 보고서"),
                null
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MILESTONE_APPEAL_LIMIT_EXCEEDED);
        verify(completionReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("3단계 완료 보고서 승인 시 최종 정산과 보증금 환급 정산을 생성하고 아이디어를 완료한다")
    void approveCompletionReport_step3_success() {
        Milestone milestone = milestone(1L, 3);
        milestone.start();
        CompletionReport report = report(1L, CompletionReportType.COMPLETION);
        given(milestoneRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(milestone));
        given(completionReportRepository.findByMilestoneIdAndType(1L, CompletionReportType.COMPLETION))
                .willReturn(Optional.of(report));
        given(ideaService.getIdeaSummary(10L)).willReturn(ideaSummary());

        CompletionReportResponse response = milestoneService.approveCompletionReport(1L);

        assertThat(response.status()).isEqualTo(CompletionReportStatus.APPROVED);
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.COMPLETED);
        verify(settlementService).createFinalSettlement(10L);
        verify(settlementService).createCompletedDepositRefundSettlement(10L);
        verify(ideaService).completeIdea(10L);
    }

    @Test
    @DisplayName("소명 3회차 보고서가 최종 반려되면 마일스톤을 취소하고 보증금 몰수 환불 흐름을 실행한다")
    void rejectReport_finalAppealRejected_cancelsMilestone() {
        Milestone milestone = milestone(1L, 2);
        milestone.start();
        CompletionReport report = report(1L, CompletionReportType.APPEAL);
        given(milestoneRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(milestone));
        given(completionReportRepository.findTopByMilestoneIdOrderBySubmittedAtDesc(1L))
                .willReturn(Optional.of(report));
        given(completionReportRepository.countByMilestoneIdAndType(1L, CompletionReportType.APPEAL))
                .willReturn(3L);
        given(ideaService.getIdeaSummary(10L)).willReturn(ideaSummary());

        CompletionReportResponse response = milestoneService.rejectReport(
                1L,
                new RejectReportRequest("최종 소명도 부족합니다.")
        );

        assertThat(response.status()).isEqualTo(CompletionReportStatus.REJECTED);
        assertThat(response.rejectReason()).isEqualTo("최종 소명도 부족합니다.");
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.CANCELLED);
        verify(ideaService).cancelIdea(10L);
        verify(settlementService).createCancelRefundSettlement(10L, "소명 3회 최종 반려");
        verify(settlementService).createDepositForfeitSettlement(10L, "소명 3회 최종 반려");
        verify(refundService).createCancelRefunds(10L, false);
    }

    @Test
    @DisplayName("펀딩 성공 확정 시 1단계 마일스톤을 시작한다")
    void startFirstMilestone_success() {
        Milestone firstMilestone = milestone(1L, 1);
        given(milestoneRepository.findByIdeaIdAndStep(10L, 1)).willReturn(Optional.of(firstMilestone));
        given(ideaService.getIdeaSummary(10L)).willReturn(ideaSummary());
        given(fundingRepository.findPaidSponsorIdsByIdeaId(10L)).willReturn(List.of(20L, 21L));

        milestoneService.startFirstMilestone(10L);

        assertThat(firstMilestone.getStatus()).isEqualTo(MilestoneStatus.IN_PROGRESS);
        verify(fundingRepository).findPaidSponsorIdsByIdeaId(10L);
    }

    private static Milestone milestone(Long id, int step) {
        Milestone milestone = Milestone.builder()
                .ideaId(10L)
                .step(step)
                .goal(step + "단계 목표")
                .expectedResult(step + "단계 결과")
                .expectedDate(LocalDate.now().plusDays(7))
                .build();
        setField(milestone, "id", id);
        return milestone;
    }

    private static CompletionReport report(Long milestoneId, CompletionReportType type) {
        return CompletionReport.builder()
                .milestoneId(milestoneId)
                .type(type)
                .content("보고서 내용")
                .fileUrl(null)
                .build();
    }

    private static IdeaSummaryResponse ideaSummary() {
        LocalDateTime now = LocalDateTime.now();
        return new IdeaSummaryResponse(
                10L,
                99L,
                "테스트 아이디어",
                "TECH",
                "한 줄 소개",
                1_000_000L,
                1_000_000L,
                2,
                now.minusDays(10),
                now.plusDays(10),
                "IN_PROGRESS",
                now.minusDays(20)
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
