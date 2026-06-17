package com.team04.domain.milestone.entity;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 마일스톤 완료 보고서 및 소명 보고서를 저장하는 엔티티입니다. */
@Entity
@Table(name = "completion_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompletionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long milestoneId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompletionReportType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompletionReportStatus status;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Builder
    private CompletionReport(Long milestoneId, CompletionReportType type, String content) {
        this.milestoneId = milestoneId;
        this.type = type;
        this.content = content;
        this.status = CompletionReportStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    /** 보고서를 승인합니다. SUBMITTED 상태에서만 가능합니다. */
    public void approve() {
        if (this.status != CompletionReportStatus.SUBMITTED) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
        }
        this.status = CompletionReportStatus.APPROVED;
    }

    /** 보고서를 반려합니다. SUBMITTED 상태에서만 가능합니다. */
    public void reject() {
        if (this.status != CompletionReportStatus.SUBMITTED) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
        }
        this.status = CompletionReportStatus.REJECTED;
    }
}