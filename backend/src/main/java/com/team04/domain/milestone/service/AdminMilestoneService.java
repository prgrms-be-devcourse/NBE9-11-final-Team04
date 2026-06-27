package com.team04.domain.milestone.service;

import com.team04.domain.milestone.dto.request.RejectReportRequest;
import com.team04.domain.milestone.dto.response.CompletionReportResponse;
import com.team04.domain.milestone.dto.response.MilestoneResponse;
import com.team04.domain.milestone.entity.CompletionReportStatus;
import com.team04.domain.milestone.repository.MilestoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminMilestoneService {

    private final MilestoneService milestoneService;
    private final MilestoneRepository milestoneRepository;

    /** 관리자 화면에서 프로젝트의 마일스톤 목록을 조회할 때 사용합니다. */
    @Transactional(readOnly = true)
    public List<MilestoneResponse> getMilestones(Long ideaId) {
        return milestoneService.getMilestones(ideaId);
    }

    /** 관리자가 검토할 보고서 목록을 조회할 때 사용합니다. */
    @Transactional(readOnly = true)
    public List<CompletionReportResponse> getReports(Long milestoneId) {
        return milestoneService.getReports(milestoneId);
    }

    /** 관리자가 보고서 상세를 확인할 때 사용합니다. */
    @Transactional(readOnly = true)
    public CompletionReportResponse getReport(Long milestoneId, Long reportId) {
        return milestoneService.getReport(milestoneId, reportId);
    }

    /** 제출된 보고서가 있어 관리자가 검토해야 하는 마일스톤을 오래된 제출 순으로 조회합니다. */
    @Transactional(readOnly = true)
    public List<MilestoneResponse> getPendingReportMilestones() {
        return milestoneRepository.findPendingReportMilestonesOrderBySubmittedAtAsc(CompletionReportStatus.SUBMITTED).stream()
                .map(MilestoneResponse::from)
                .toList();
    }

    /** 완료 보고서 승인 흐름을 기존 마일스톤 서비스에 위임합니다. */
    @Transactional
    public CompletionReportResponse approveCompletionReport(Long milestoneId) {
        return milestoneService.approveCompletionReport(milestoneId);
    }

    /** 소명 보고서 승인 흐름을 기존 마일스톤 서비스에 위임합니다. */
    @Transactional
    public CompletionReportResponse approveAppealReport(Long milestoneId) {
        return milestoneService.approveAppealReport(milestoneId);
    }

    /** 보고서 반려 흐름을 기존 마일스톤 서비스에 위임합니다. */
    @Transactional
    public CompletionReportResponse rejectReport(Long milestoneId, RejectReportRequest request) {
        return milestoneService.rejectReport(milestoneId, request);
    }

    /** 소명 중단 인정 및 환불 흐름을 기존 마일스톤 서비스에 위임합니다. */
    @Transactional
    public void refundMilestone(Long milestoneId) {
        milestoneService.refundMilestone(milestoneId);
    }

    /** 이행 중단 흐름을 기존 마일스톤 서비스에 위임합니다. */
    @Transactional
    public void cancelMilestone(Long ideaId) {
        milestoneService.cancelMilestone(ideaId);
    }
}
