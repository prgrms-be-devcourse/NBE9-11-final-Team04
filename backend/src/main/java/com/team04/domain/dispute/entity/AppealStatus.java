package com.team04.domain.dispute.entity;

public enum AppealStatus {
    SUBMITTED,  // 소명 제출됨 (검토 대기)
    APPROVED,   // 소명 수용됨 (신고 기각)
    REJECTED    // 소명 거절됨 (재제출 필요 또는 최종 실패)
}
