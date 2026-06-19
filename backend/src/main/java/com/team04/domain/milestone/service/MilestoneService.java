package com.team04.domain.milestone.service;

import com.team04.domain.milestone.dto.request.CompletionReportRequest;
import com.team04.domain.milestone.dto.response.CompletionReportResponse;
import com.team04.domain.milestone.entity.CompletionReport;
import com.team04.domain.milestone.entity.CompletionReportStatus;
import com.team04.domain.milestone.entity.CompletionReportType;
import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.entity.MilestoneStatus;
import com.team04.domain.milestone.repository.CompletionReportRepository;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.domain.settlement.service.SettlementService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final CompletionReportRepository completionReportRepository;
    private final SettlementService settlementService;

    /**
     * 완료 보고서 제출
     * 제안자만 가능, 마일스톤이 IN_PROGRESS 상태여야 함
     * 완료 보고서가 이미 존재하면 중복 제출 불가
     */
    @Transactional
    public CompletionReportResponse submitCompletionReport(Long milestoneId, CompletionReportRequest request) {
        Milestone milestone = findMilestone(milestoneId);

        if (milestone.getStatus() != MilestoneStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
        }

        if (completionReportRepository.findByMilestoneIdAndType(milestoneId, CompletionReportType.COMPLETION).isPresent()) {
            throw new CustomException(ErrorCode.MILESTONE_ALREADY_COMPLETED);
        }

        CompletionReport report = CompletionReport.builder()
                .milestoneId(milestoneId)
                .type(CompletionReportType.COMPLETION)
                .content(request.content())
                .build();

        return CompletionReportResponse.from(completionReportRepository.save(report));
    }

    /**
     * 소명 보고서 제출
     * 제안자만 가능, 완료 보고서가 REJECTED 상태여야 함
     * 소명 보고서가 이미 존재하면 중복 제출 불가
     */
    @Transactional
    public CompletionReportResponse submitAppealReport(Long milestoneId, CompletionReportRequest request) {
        CompletionReport completionReport = completionReportRepository
                .findByMilestoneIdAndType(milestoneId, CompletionReportType.COMPLETION)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION));

        if (completionReport.getStatus() != CompletionReportStatus.REJECTED) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
        }

        if (completionReportRepository.findByMilestoneIdAndType(milestoneId, CompletionReportType.APPEAL).isPresent()) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
        }

        CompletionReport appealReport = CompletionReport.builder()
                .milestoneId(milestoneId)
                .type(CompletionReportType.APPEAL)
                .content(request.content())
                .build();

        return CompletionReportResponse.from(completionReportRepository.save(appealReport));
    }

    /**
     * 완료 보고서 승인
     * 관리자만 가능
     * 3단계 완료 승인 시 최종 정산 생성
     * 3단계 미만 완료 승인 시 다음 마일스톤 자동 시작 (단일 트랜잭션)
     */
    @Transactional
    public CompletionReportResponse approveReport(Long milestoneId) {
        Milestone milestone = findMilestone(milestoneId);

        CompletionReport report = findLatestReport(milestoneId);
        report.approve();
        milestone.complete();

        if (milestone.getStep() == 3) {
            settlementService.createFinalSettlement(milestone.getIdeaId());
        } else {
            startNextMilestone(milestone.getIdeaId(), milestone.getStep() + 1);
        }

        return CompletionReportResponse.from(report);
    }

    /**
     * 완료 보고서 반려
     * 관리자만 가능, 제안자에게 소명 보고서 요청
     */
    @Transactional
    public CompletionReportResponse rejectReport(Long milestoneId) {
        findMilestone(milestoneId);

        CompletionReport report = findLatestReport(milestoneId);
        report.reject();

        return CompletionReportResponse.from(report);
    }

    /**
     * 펀딩 목표 달성 시 1단계 마일스톤 자동 시작
     * TODO: 펀딩 도메인 담당자에게 FundingSuccessEvent 발행 요청 필요
     */
    @Transactional
    public void startFirstMilestone(Long ideaId) {
        startNextMilestone(ideaId, 1);
    }

    /**
     * 이행 중단 처리
     * 관리자만 가능
     * 현재 IN_PROGRESS 마일스톤 CANCELLED 전환 후 환불 장부 생성
     */
    @Transactional
    public void cancelMilestone(Long ideaId) {
        Milestone milestone = milestoneRepository.findByIdeaIdAndStatus(ideaId, MilestoneStatus.IN_PROGRESS)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
        milestone.cancel();
        settlementService.createRefundSettlement(ideaId);
    }

    /**
     * 다음 단계 마일스톤을 IN_PROGRESS로 전이
     * 현재 트랜잭션 내에서 실행 — 중간 실패 시 전체 롤백
     */
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