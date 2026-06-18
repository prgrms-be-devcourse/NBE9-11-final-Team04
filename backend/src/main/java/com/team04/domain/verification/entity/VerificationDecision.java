package com.team04.domain.verification.entity;

/** AI 검증 또는 운영 검토의 최종 판단 결과를 표현하는 열거형입니다. */
public enum VerificationDecision {

    /** 검증을 통과한 판단입니다. */
    PASS,

    /** 보완 요청이 필요한 판단입니다. */
    NEEDS_REVISION,

    /** 반려가 필요한 판단입니다. */
    REJECT,

    /** 관리자 검토로 이관해야 하는 판단입니다. */
    PENDING_ADMIN_REVIEW
}
