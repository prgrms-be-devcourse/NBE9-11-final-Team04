package com.team04.domain.milestone.entity;

/** 완료 보고서 처리 상태를 관리하는 열거형입니다. */
public enum CompletionReportStatus {

    /** 제출됨 (관리자 검토 대기) */
    SUBMITTED,

    /** 관리자 승인 완료 */
    APPROVED,

    /** 관리자 반려 (소명 보고서 요청) */
    REJECTED
}