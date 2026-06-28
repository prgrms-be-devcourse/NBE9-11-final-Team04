package com.team04.domain.milestone.service;

import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.funding.service.FundingService;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.milestone.dto.request.CompletionReportRequest;
import com.team04.domain.milestone.dto.request.RejectReportRequest;
import com.team04.domain.milestone.dto.response.CompletionReportResponse;
import com.team04.domain.milestone.dto.response.MilestoneResponse;
import com.team04.domain.milestone.entity.CompletionReport;
import com.team04.domain.milestone.entity.CompletionReportStatus;
import com.team04.domain.milestone.entity.CompletionReportType;
import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.entity.MilestoneStatus;
import com.team04.domain.milestone.repository.CompletionReportRepository;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.settlement.service.RefundService;
import com.team04.domain.settlement.service.SettlementService;
import com.team04.global.event.NotificationEvent;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.storage.MilestoneReportStorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private static final String COMPLETION_REPORT_DIR = "completion";
    private static final String APPEAL_REPORT_DIR = "appeal";
    private static final int MAX_APPEAL_REPORT_COUNT = 3;

    private final MilestoneRepository milestoneRepository;
    private final CompletionReportRepository completionReportRepository;
    private final IdeaService ideaService;
    private final SettlementService settlementService;
    private final RefundService refundService;
    private final FundingService fundingService;
    private final MilestoneReportStorageClient milestoneReportStorageClient;
    private final FundingRepository fundingRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 마일스톤 목록 조회
     * ideaId 기준으로 해당 프로젝트의 마일스톤 전체를 단계 순으로 반환
     */
    @Transactional(readOnly = true)
    public List<MilestoneResponse> getMilestones(Long ideaId) {
        return milestoneRepository.findByIdeaIdOrderByStep(ideaId)
                .stream()
                .map(MilestoneResponse::from)
                .toList();
    }

    /**
     * 마일스톤 단건 조회
     */
    @Transactional(readOnly = true)
    public MilestoneResponse getMilestone(Long milestoneId) {
        return MilestoneResponse.from(findMilestone(milestoneId));
    }

    /**
     * 완료/소명 보고서 단건 조회
     * 로그인한 사용자 누구나 가능
     */
    @Transactional(readOnly = true)
    public CompletionReportResponse getReport(Long milestoneId, Long reportId) {
        if (!milestoneRepository.existsById(milestoneId)) {
            throw new CustomException(ErrorCode.MILESTONE_NOT_FOUND);
        }
        CompletionReport report = completionReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPLETION_REPORT_NOT_FOUND));
        if (!report.getMilestoneId().equals(milestoneId)) {
            throw new CustomException(ErrorCode.COMPLETION_REPORT_MISMATCH);
        }
        return CompletionReportResponse.from(report, milestoneReportStorageClient);
    }

    /**
     * 완료/소명 보고서 목록 조회
     * milestoneId 기준으로 해당 마일스톤의 보고서 전체를 최신순으로 반환
     */
    @Transactional(readOnly = true)
    public List<CompletionReportResponse> getReports(Long milestoneId) {
        if (!milestoneRepository.existsById(milestoneId)) {
            throw new CustomException(ErrorCode.MILESTONE_NOT_FOUND);
        }
        return completionReportRepository.findByMilestoneIdOrderBySubmittedAtDesc(milestoneId)
                .stream()
                .map(report -> CompletionReportResponse.from(report, milestoneReportStorageClient))
                .toList();
    }

    /**
     * 완료 보고서 제출
     * 제안자만 가능, 마일스톤이 IN_PROGRESS 상태여야 함
     * 완료 보고서가 이미 존재하면 중복 제출 불가
     * 파일 첨부는 선택 사항
     * 완료 보고서 제출 시 overdueAt 초기화 — 기한 초과 후 제출해도 몰수 대상에서 제외
     */
    @Transactional
    public CompletionReportResponse submitCompletionReport(
            Long milestoneId, CompletionReportRequest request, MultipartFile file) {
        Milestone milestone = findMilestone(milestoneId);
        ideaService.validateNotSuspended(milestone.getIdeaId()); // 분쟁 처리 중 일시 중단된 프로젝트는 완료 보고서 제출 불가

        if (milestone.getStatus() != MilestoneStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
        }

        if (completionReportRepository.findByMilestoneIdAndType(milestoneId, CompletionReportType.COMPLETION).isPresent()) {
            throw new CustomException(ErrorCode.MILESTONE_ALREADY_COMPLETED);
        }

        String fileKey = uploadFileIfPresent(file, COMPLETION_REPORT_DIR);
        milestone.clearOverdue();

        CompletionReport report = CompletionReport.builder()
                .milestoneId(milestoneId)
                .type(CompletionReportType.COMPLETION)
                .content(request.content())
                .fileUrl(fileKey)
                .build();

        return CompletionReportResponse.from(completionReportRepository.save(report), milestoneReportStorageClient);
    }

    /**
     * 소명 보고서 제출
     * 제안자만 가능, 최신 완료/소명 보고서가 REJECTED 상태여야 함
     * 소명 보고서가 반려된 뒤에는 최대 3회까지 다시 제출할 수 있음
     * 소명 보고서 제출 시 overdueAt 초기화 — 먹튀 아님으로 판단
     * 파일 첨부는 선택 사항
     */
    @Transactional
    public CompletionReportResponse submitAppealReport(
            Long milestoneId, CompletionReportRequest request, MultipartFile file) {
        Milestone milestone = findMilestone(milestoneId);

        CompletionReport latestReport = findLatestReport(milestoneId);
        if (latestReport.getStatus() != CompletionReportStatus.REJECTED) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
        }
        validateAppealReportLimit(milestoneId);

        String fileKey = uploadFileIfPresent(file, APPEAL_REPORT_DIR);
        milestone.clearOverdue();

        CompletionReport appealReport = CompletionReport.builder()
                .milestoneId(milestoneId)
                .type(CompletionReportType.APPEAL)
                .content(request.content())
                .fileUrl(fileKey)
                .build();

        return CompletionReportResponse.from(completionReportRepository.save(appealReport), milestoneReportStorageClient);
    }

    private void validateAppealReportLimit(Long milestoneId) {
        long appealCount = completionReportRepository.countByMilestoneIdAndType(
                milestoneId,
                CompletionReportType.APPEAL
        );
        if (appealCount >= MAX_APPEAL_REPORT_COUNT) {
            throw new CustomException(ErrorCode.MILESTONE_APPEAL_LIMIT_EXCEEDED);
        }
    }

    /**
     * 완료 보고서 승인 (정상 진행)
     * 관리자만 가능
     * 3단계 완료 승인 시 최종 정산 + 보증금 전액 환급 생성
     * 3단계 미만 완료 승인 시 다음 마일스톤 자동 시작
     */
    @Transactional
    public CompletionReportResponse approveCompletionReport(Long milestoneId) {
        Milestone milestone = milestoneRepository.findByIdWithPessimisticLock(milestoneId)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));

        CompletionReport report = completionReportRepository
                .findByMilestoneIdAndType(milestoneId, CompletionReportType.COMPLETION)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION));

        report.approve();
        milestone.complete();
        notifyReportApproved(milestone, report);

        if (milestone.getStep() == 3) {
            settlementService.createFinalSettlement(milestone.getIdeaId());
            settlementService.createCompletedDepositRefundSettlement(milestone.getIdeaId());
        } else {
            startNextMilestone(milestone.getIdeaId(), milestone.getStep() + 1);
        }

        return CompletionReportResponse.from(report, milestoneReportStorageClient);
    }

    /**
     * 소명 보고서 승인 (계속 진행 인정)
     * 관리자만 가능
     * 소명 인정 후 다음 마일스톤 자동 시작
     */
    @Transactional
    public CompletionReportResponse approveAppealReport(Long milestoneId) {
        Milestone milestone = milestoneRepository.findByIdWithPessimisticLock(milestoneId)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));

        CompletionReport report = completionReportRepository
                .findTopByMilestoneIdAndTypeOrderBySubmittedAtDesc(milestoneId, CompletionReportType.APPEAL)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION));

        report.approve();
        milestone.complete();
        notifyReportApproved(milestone, report);

        if (milestone.getStep() == 3) {
            // 3단계 소명 승인 = 최종 완성으로 처리
            settlementService.createFinalSettlement(milestone.getIdeaId());
            settlementService.createCompletedDepositRefundSettlement(milestone.getIdeaId());
        } else {
            startNextMilestone(milestone.getIdeaId(), milestone.getStep() + 1);
        }

        return CompletionReportResponse.from(report, milestoneReportStorageClient);
    }

    /**
     * 완료/소명 보고서 반려
     * 관리자만 가능
     * 최신 보고서를 반려하고 제안자에게 반려 알림을 예약합니다.
     * 소명 보고서가 3회까지 반려되면 더 이상 보완 기회가 없으므로 보증금 몰수 중단 흐름으로 전환합니다.
     */
    @Transactional
    public CompletionReportResponse rejectReport(Long milestoneId, RejectReportRequest request) {
        Milestone milestone = milestoneRepository.findByIdWithPessimisticLock(milestoneId)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
        if (milestone.getStatus() != MilestoneStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
        }

        CompletionReport report = findLatestReport(milestoneId);
        if (report.getStatus() != CompletionReportStatus.SUBMITTED) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
        }

        report.reject(request.reason());
        notifyReportRejected(milestone, report);
        cancelIfFinalAppealRejected(milestone, report);
        return CompletionReportResponse.from(report, milestoneReportStorageClient);
    }

    /**
     * 소명 중단 인정 + 환불 처리
     * 관리자만 가능
     * "더 이상 진행 못하겠다"는 소명을 관리자가 인정할 때 호출
     * 최신 보고서가 APPEAL 타입이어야 함 — COMPLETION 타입이면 예외 발생
     * 마일스톤 CANCELLED 전환 후 환불 장부 + 후원자별 환불 레코드 생성
     */
    @Transactional
    public void refundMilestone(Long milestoneId) {
        Milestone milestone = milestoneRepository.findByIdWithPessimisticLock(milestoneId)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));

        CompletionReport report = findLatestReport(milestoneId);

        if (report.getType() != CompletionReportType.APPEAL) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
        }

        report.approve();
        milestone.cancel();

        settlementService.createJustifiedCancelRefundSettlement(milestone.getIdeaId());
        settlementService.createDepositRefundSettlement(milestone.getIdeaId());
        refundService.createCancelRefunds(milestone.getIdeaId(), true); // 정당한 사유 — 보증금 잔액 제안자 환급
    }

    /**
     * 보증금 몰수 처리 (먹튀/잠수)
     * MilestoneScheduler에서 3일 유예기간 경과 후 호출
     * 비관락 획득 후 SUBMITTED 보고서 존재 여부 재검증 — 소명 보고서 제출 시 처리 중단
     * 마일스톤 CANCELLED 전환 후 보증금 몰수 정산 + 환불 처리
     */
    @Transactional
    public void forfeitMilestone(Long ideaId) {
        Milestone milestone = milestoneRepository.findByIdeaIdAndStatusWithPessimisticLock(ideaId, MilestoneStatus.IN_PROGRESS)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));

        if (completionReportRepository.existsByMilestoneIdAndStatus(milestone.getId(), CompletionReportStatus.SUBMITTED)) {
            return;
        }

        cancelMilestoneAsUnjustified(milestone, null);
    }

    /**
     * 수동 이행 중단 처리
     * 관리자만 가능
     */
    @Transactional
    public void cancelMilestone(Long ideaId) {
        Milestone milestone = milestoneRepository.findByIdeaIdAndStatusWithPessimisticLock(ideaId, MilestoneStatus.IN_PROGRESS)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
        cancelMilestoneAsUnjustified(milestone, null);
    }

    private void cancelIfFinalAppealRejected(Milestone milestone, CompletionReport report) {
        if (report.getType() != CompletionReportType.APPEAL) {
            return;
        }

        long appealCount = completionReportRepository.countByMilestoneIdAndType(
                milestone.getId(),
                CompletionReportType.APPEAL
        );
        if (appealCount < MAX_APPEAL_REPORT_COUNT) {
            return;
        }

        // 소명은 최대 3회까지 허용한다. 3회차 소명까지 반려되면 보완 기회를 모두 사용한 것으로 보고,
        // 정당한 중단 사유가 인정되지 않은 상태이므로 프로젝트를 중단하고 보증금을 몰수한다.
        cancelMilestoneAsUnjustified(milestone, "소명 3회 최종 반려");
    }

    private void cancelMilestoneAsUnjustified(Milestone milestone, String memo) {
        Long ideaId = milestone.getIdeaId();
        milestone.cancel();
        settlementService.createCancelRefundSettlement(ideaId, memo);        // 부정/미소명 중단 — 후원금 잔액
        settlementService.createDepositForfeitSettlement(ideaId, memo);      // 부정/미소명 중단 — 보증금 몰수
        refundService.createCancelRefunds(ideaId, false); // 부정/미소명 중단 — 보증금 전액 후원자 분배
    }

    /**
     * 펀딩 목표 달성 시 1단계 마일스톤 자동 시작
     * FundingAchievementListener에서 FundingPaidEvent 수신 후 목표 달성 확인 시 호출
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startFirstMilestone(Long ideaId) {
        startNextMilestone(ideaId, 1);
    }


    private String uploadFileIfPresent(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        // 완료/소명 보고서는 공개 이미지 URL이 아니라 보안 스토리지 객체 key로 저장합니다.
        // 조회 응답에서만 짧은 만료 시간을 가진 접근 URL로 변환합니다.
        return milestoneReportStorageClient.upload(file, subDirectory);
    }

    private void startNextMilestone(Long ideaId, int step) {
        Milestone nextMilestone = milestoneRepository.findByIdeaIdAndStep(ideaId, step)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
        nextMilestone.start();
        notifyMilestoneStarted(nextMilestone);
    }

    /**
     * 새 단계가 시작되면 제안자와 실제 결제 성공 후원자에게 진행 단계 변경을 알립니다.
     * 후원자 중복 발송을 막기 위해 sponsorId를 DISTINCT로 조회합니다.
     */
    private void notifyMilestoneStarted(Milestone milestone) {
        IdeaResponse idea = ideaService.getIdea(milestone.getIdeaId());
        Set<Long> targetUserIds = new LinkedHashSet<>();
        targetUserIds.add(idea.userId());
        targetUserIds.addAll(fundingRepository.findPaidSponsorIdsByIdeaId(milestone.getIdeaId()));

        String title = "마일스톤 " + milestone.getStep() + "단계가 시작되었습니다";
        String message = "'" + idea.title() + "' 프로젝트의 " + milestone.getStep()
                + "단계가 시작되었습니다. 목표: " + milestone.getGoal();

        notifyUsers(targetUserIds, NotificationType.MILESTONE_STARTED, title, message, milestone.getId());
    }

    /**
     * 완료/소명 보고서 승인 결과는 실제 작업 당사자인 제안자에게만 알립니다.
     * 보고서 타입은 알림 메시지에서 완료 보고서/소명 보고서로 구분합니다.
     */
    private void notifyReportApproved(Milestone milestone, CompletionReport report) {
        IdeaResponse idea = ideaService.getIdea(milestone.getIdeaId());
        String reportName = report.getType() == CompletionReportType.APPEAL ? "소명 보고서" : "완료 보고서";
        String title = "마일스톤 " + milestone.getStep() + "단계 " + reportName + "가 승인되었습니다";
        String message = "'" + idea.title() + "' 프로젝트의 " + milestone.getStep()
                + "단계 " + reportName + "가 승인되었습니다.";

        notifyUsers(Set.of(idea.userId()), NotificationType.MILESTONE_REPORT_APPROVED,
                title, message, report.getId());
    }

    /**
     * 완료/소명 보고서 반려 결과는 제안자에게만 알립니다.
     * 반려 사유는 응답 상세에서 확인할 수 있도록 별도 필드로 내려줍니다.
     */
    private void notifyReportRejected(Milestone milestone, CompletionReport report) {
        IdeaResponse idea = ideaService.getIdea(milestone.getIdeaId());
        String reportName = report.getType() == CompletionReportType.APPEAL ? "소명 보고서" : "완료 보고서";
        String title = "마일스톤 " + milestone.getStep() + "단계 " + reportName + "가 반려되었습니다";
        String message = "'" + idea.title() + "' 프로젝트의 " + milestone.getStep()
                + "단계 " + reportName + "가 반려되었습니다. 반려 사유를 확인해 주세요.";

        notifyUsers(Set.of(idea.userId()), NotificationType.MILESTONE_REPORT_REJECTED,
                title, message, report.getId());
    }

    /**
     * 알림 발송 자체는 outbox 스케줄러에 맡깁니다.
     * 현재 트랜잭션이 실패하면 이벤트로 생성되는 outbox 기록도 함께 롤백됩니다.
     */
    private void notifyUsers(Set<Long> userIds, NotificationType type, String title, String message, Long referenceId) {
        for (Long userId : userIds) {
            if (userId != null) {
                // 알림은 현재 트랜잭션 커밋 전에 outbox로만 기록하고, 실제 발송은 커밋 이후 스케줄러가 처리한다.
                // 뒤쪽 정산/마일스톤 작업이 실패했는데 사용자에게 먼저 알림이 보이는 상황을 막기 위함이다.
                eventPublisher.publishEvent(new NotificationEvent(userId, type, title, message, referenceId));
            }
        }
    }

    private Milestone findMilestone(Long milestoneId) {
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
    }

    private CompletionReport findLatestReport(Long milestoneId) {
        return completionReportRepository
                .findTopByMilestoneIdOrderBySubmittedAtDesc(milestoneId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION));
    }
}
