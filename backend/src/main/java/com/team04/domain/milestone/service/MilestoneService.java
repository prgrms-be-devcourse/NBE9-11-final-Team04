package com.team04.domain.milestone.service;

import com.team04.domain.milestone.dto.request.CompletionReportRequest;
import com.team04.domain.milestone.dto.response.CompletionReportResponse;
import com.team04.domain.milestone.entity.CompletionReport;
import com.team04.domain.milestone.entity.CompletionReportStatus;
import com.team04.domain.milestone.entity.CompletionReportType;
import com.team04.domain.milestone.entity.Milestone;
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

        if (milestone.getStatus() != com.team04.domain.milestone.entity.MilestoneStatus.IN_PROGRESS) {
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
     */
    @Transactional
    public CompletionReportResponse submitAppealReport(Long milestoneId, CompletionReportRequest request) {
        CompletionReport completionReport = completionReportRepository
                .findByMilestoneIdAndType(milestoneId, CompletionReportType.COMPLETION)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));

        if (completionReport.getStatus() != CompletionReportStatus.REJECTED) {
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
     * 마일스톤 3단계 완료 승인 시 최종 정산 생성
     */
    @Transactional
    public CompletionReportResponse approveReport(Long milestoneId) {
        Milestone milestone = findMilestone(milestoneId);

        CompletionReport report = findLatestReport(milestoneId);
        report.approve();
        milestone.complete();

        if (milestone.getStep() == 3) {
            settlementService.createFinalSettlement(milestone.getIdeaId());
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

    private Milestone findMilestone(Long milestoneId) {
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
    }

    private CompletionReport findLatestReport(Long milestoneId) {
        return completionReportRepository
                .findByMilestoneIdAndType(milestoneId, CompletionReportType.APPEAL)
                .orElseGet(() -> completionReportRepository
                        .findByMilestoneIdAndType(milestoneId, CompletionReportType.COMPLETION)
                        .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND)));
    }
}