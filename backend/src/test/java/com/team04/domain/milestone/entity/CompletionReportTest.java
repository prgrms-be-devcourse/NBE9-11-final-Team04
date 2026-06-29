package com.team04.domain.milestone.entity;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompletionReportTest {

    @Test
    @DisplayName("제출된 보고서를 승인할 수 있다")
    void approve_success() {
        CompletionReport report = createReport();

        report.approve();

        assertThat(report.getStatus()).isEqualTo(CompletionReportStatus.APPROVED);
    }

    @Test
    @DisplayName("제출된 보고서를 반려 사유와 함께 반려할 수 있다")
    void reject_success() {
        CompletionReport report = createReport();

        report.reject("증빙 자료가 부족합니다.");

        assertThat(report.getStatus()).isEqualTo(CompletionReportStatus.REJECTED);
        assertThat(report.getRejectReason()).isEqualTo("증빙 자료가 부족합니다.");
    }

    @Test
    @DisplayName("반려 사유가 비어 있으면 보고서를 반려할 수 없다")
    void reject_blankReason_throwsException() {
        CompletionReport report = createReport();

        assertThatThrownBy(() -> report.reject(" "))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("이미 승인된 보고서는 다시 반려할 수 없다")
    void reject_approvedReport_throwsException() {
        CompletionReport report = createReport();
        report.approve();

        assertThatThrownBy(() -> report.reject("반려 사유"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
    }

    private static CompletionReport createReport() {
        return CompletionReport.builder()
                .milestoneId(1L)
                .type(CompletionReportType.COMPLETION)
                .content("보고서 내용")
                .fileUrl(null)
                .build();
    }
}
