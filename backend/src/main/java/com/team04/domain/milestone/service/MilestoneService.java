package com.team04.domain.milestone.service;

import com.team04.domain.funding.service.FundingService;
import com.team04.domain.milestone.dto.request.CompletionReportRequest;
import com.team04.domain.milestone.dto.response.CompletionReportResponse;
import com.team04.domain.milestone.dto.response.MilestoneResponse;
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
import com.team04.global.storage.StorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private static final String REPORT_STORAGE_DIR = "milestone/reports";

    private final MilestoneRepository milestoneRepository;
    private final CompletionReportRepository completionReportRepository;
    private final SettlementService settlementService;
    private final RefundService refundService;
    private final FundingService fundingService;
    private final StorageClient storageClient;

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
                .map(CompletionReportResponse::from)
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

        if (milestone.getStatus() != MilestoneStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
        }

        if (completionReportRepository.findByMilestoneIdAndType(milestoneId, CompletionReportType.COMPLETION).isPresent()) {
            throw new CustomException(ErrorCode.MILESTONE_ALREADY_COMPLETED);
        }

        String fileUrl = uploadFileIfPresent(file);
        milestone.clearOverdue();

        CompletionReport report = CompletionReport.builder()
                .milestoneId(milestoneId)
                .type(CompletionReportType.COMPLETION)
                .content(request.content())
                .fileUrl(fileUrl)
                .build();

        return CompletionReportResponse.from(completionReportRepository.save(report));
    }

    /**
     * 소명 보고서 제출
     * 제안자만 가능, 완료 보고서가 REJECTED 상태여야 함
     * 소명 보고서가 이미 존재하면 중복 제출 불가
     * 소명 보고서 제출 시 overdueAt 초기화 — 먹튀 아님으로 판단
     * 파일 첨부는 선택 사항
     */
    @Transactional
    public CompletionReportResponse submitAppealReport(
            Long milestoneId, CompletionReportRequest request, MultipartFile file) {
        Milestone milestone = findMilestone(milestoneId);

        CompletionReport completionReport = completionReportRepository
                .findByMilestoneIdAndType(milestoneId, CompletionReportType.COMPLETION)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION));

        if (completionReport.getStatus() != CompletionReportStatus.REJECTED) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
        }

        if (completionReportRepository.findByMilestoneIdAndType(milestoneId, CompletionReportType.APPEAL).isPresent()) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
        }

        String fileUrl = uploadFileIfPresent(file);
        milestone.clearOverdue();

        CompletionReport appealReport = CompletionReport.builder()
                .milestoneId(milestoneId)
                .type(CompletionReportType.APPEAL)
                .content(request.content())
                .fileUrl(fileUrl)
                .build();

        return CompletionReportResponse.from(completionReportRepository.save(appealReport));
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

        if (milestone.getStep() == 3) {
            settlementService.createFinalSettlement(milestone.getIdeaId());
            settlementService.createCompletedDepositRefundSettlement(milestone.getIdeaId());
            fundingService.releaseDeposit(milestone.getIdeaId());
        } else {
            startNextMilestone(milestone.getIdeaId(), milestone.getStep() + 1);
        }

        return CompletionReportResponse.from(report);
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
                .findByMilestoneIdAndType(milestoneId, CompletionReportType.APPEAL)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION));

        report.approve();
        milestone.complete();

        if (milestone.getStep() == 3) {
            // 3단계 소명 승인 = 최종 완성으로 처리
            settlementService.createFinalSettlement(milestone.getIdeaId());
            settlementService.createCompletedDepositRefundSettlement(milestone.getIdeaId());
            fundingService.releaseDeposit(milestone.getIdeaId());
        } else {
            startNextMilestone(milestone.getIdeaId(), milestone.getStep() + 1);
        }

        return CompletionReportResponse.from(report);
    }

    /**
     * 완료/소명 보고서 반려
     * 관리자만 가능
     */
    @Transactional
    public CompletionReportResponse rejectReport(Long milestoneId) {
        findMilestone(milestoneId);
        CompletionReport report = findLatestReport(milestoneId);
        report.reject();
        return CompletionReportResponse.from(report);
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

        milestone.cancel();
        settlementService.createCancelRefundSettlement(ideaId);        // 먹튀/잠수 — 후원금 잔액
        settlementService.createDepositForfeitSettlement(ideaId);      // 먹튀/잠수 — 보증금 몰수
        refundService.createCancelRefunds(ideaId, false); // 먹튀/잠수 — 보증금 전액 후원자 분배
    }

    /**
     * 수동 이행 중단 처리
     * 관리자만 가능
     */
    @Transactional
    public void cancelMilestone(Long ideaId) {
        Milestone milestone = milestoneRepository.findByIdeaIdAndStatusWithPessimisticLock(ideaId, MilestoneStatus.IN_PROGRESS)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
        milestone.cancel();
        settlementService.createCancelRefundSettlement(ideaId);        // 수동 중단 — 후원금 잔액
        settlementService.createDepositForfeitSettlement(ideaId);      // 수동 중단 — 보증금 몰수
        refundService.createCancelRefunds(ideaId, false); // 수동 중단 — 보증금 전액 후원자 분배
    }

    /**
     * 펀딩 목표 달성 시 1단계 마일스톤 자동 시작
     * FundingAchievementListener에서 FundingPaidEvent 수신 후 목표 달성 확인 시 호출
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startFirstMilestone(Long ideaId) {
        startNextMilestone(ideaId, 1);
    }


    private String uploadFileIfPresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return storageClient.upload(file, REPORT_STORAGE_DIR);
    }

    private void startNextMilestone(Long ideaId, int step) {
        Milestone nextMilestone = milestoneRepository.findByIdeaIdAndStep(ideaId, step)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
        nextMilestone.start();
    }

    private Milestone findMilestone(Long milestoneId) {
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
    }

    private CompletionReport findLatestReport(Long milestoneId) {
        return completionReportRepository
                .findByMilestoneIdAndType(milestoneId, CompletionReportType.APPEAL)
                .orElseGet(() -> completionReportRepository
                        .findByMilestoneIdAndType(milestoneId, CompletionReportType.COMPLETION)
                        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION)));
    }
}