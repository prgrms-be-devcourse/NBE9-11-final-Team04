package com.team04.domain.notification.entity;

public enum NotificationType {
    IDEA_AI_APPROVED,                   // AI 검증 통과 → 제안자
    IDEA_AI_REJECTED,                   // AI 검증 반려 → 제안자
    IDEA_ADMIN_APPROVED,                // 관리자 최종 승인 → 제안자
    IDEA_ADMIN_REJECTED,                // 관리자 반려 → 제안자
    IDEA_FUNDING_GOAL_MET,              // 목표금액 달성 → 제안자
    IDEA_FUNDING_CLOSING_SOON,          // 마감 7일 전 → 제안자

    MATCH_REQUESTED,                    // 매칭 요청 받음 → 전문가
    MATCH_ACCEPTED,                     // 매칭 수락됨 → 제안자
    MATCH_REJECTED,                     // 매칭 거절됨 → 제안자

    MILESTONE_APPROVED,                 // 마일스톤 승인 → 제안자
    MILESTONE_REJECTED,                 // 마일스톤 반려 → 제안자

    REPORT_RECEIVED,                   // 신고 접수됨 → 관리자
    DISPUTE_UNDER_REVIEW,              // 신고 검토 시작 → 피신고자
    DISPUTE_RESOLVED,                  // 분쟁 처리 완료 → 신고자 + 피신고자
    DISPUTE_REJECTED,                  // 신고 기각 → 신고자 + 피신고자

    ANNOUNCEMENT,                       // 관리자 공지 → 역할별/전체 사용자

    EXPERT_SUSPENDED,               // 전문가 계정 격리 → 전문가
    EXPERT_REVERIFICATION_REQUIRED  // 국가자격증 서류 재제출 요청 → 전문가

}
