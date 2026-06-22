package com.team04.domain.milestone.dto.response;

import com.team04.domain.milestone.entity.CompletionReport;
import com.team04.domain.milestone.entity.CompletionReportStatus;
import com.team04.domain.milestone.entity.CompletionReportType;

import java.time.LocalDateTime;

/** 완료 보고서 응답 DTO입니다. */
public record CompletionReportResponse(
        Long reportId,
        Long milestoneId,
        CompletionReportType type,
        String content,
        String fileUrl,
        CompletionReportStatus status,
        LocalDateTime submittedAt
) {
    public static CompletionReportResponse from(CompletionReport report) {
        return new CompletionReportResponse(
                report.getId(),
                report.getMilestoneId(),
                report.getType(),
                report.getContent(),
                report.getFileUrl(),
                report.getStatus(),
                report.getSubmittedAt()
        );
    }
}