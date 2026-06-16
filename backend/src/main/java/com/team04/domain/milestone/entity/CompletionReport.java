package com.team04.domain.milestone.entity;

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

    /** 보고서를 승인합니다. */
    public void approve() {
        this.status = CompletionReportStatus.APPROVED;
    }

    /** 보고서를 반려합니다. */
    public void reject() {
        this.status = CompletionReportStatus.REJECTED;
    }
}