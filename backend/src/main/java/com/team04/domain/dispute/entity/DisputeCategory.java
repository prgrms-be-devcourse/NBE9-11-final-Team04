package com.team04.domain.dispute.entity;

public enum DisputeCategory {
    MILESTONE_NEGLIGENCE,       // 마일스톤 미이행
    FUND_MISUSE,                // 자금 횡령/유용
    FALSE_COMPLETION_REPORT,    // 완료 보고서 허위
    FALSE_APPEAL_REPORT,        // 소명 보고서 허위
    FRAUDULENT_PROJECT,         // 허위/사기 프로젝트
    UNJUST_CANCELLATION,        // 일방적 프로젝트 취소
    MALICIOUS_REFUND,           // 악의적 환불 요청
    WORKSPACE_MISCONDUCT,       // 워크스페이스 내 부적절한 언행
    BIASED_VERIFICATION,        // 부당한 검증 결과
    IDEA_THEFT,                 // 아이디어 탈취
    FAKE_CREDENTIALS,           // 전문가 자격 위조
    VERIFICATION_OBSTRUCTION,   // 검증 과정 방해
    PRIVACY_VIOLATION,          // 개인정보 침해
    INAPPROPRIATE_CONTENT,      // 부적절 콘텐츠
    ACCOUNT_IMPERSONATION       // 계정 도용/사칭
}
