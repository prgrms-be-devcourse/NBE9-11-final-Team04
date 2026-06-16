package com.team04.domain.verification.entity;

/** 검증 결과가 어떤 항목에서 발생했는지 식별하는 코드 열거형입니다. */
public enum VerificationCheckCode {

    /** 과장 광고 표현 검증 항목입니다. */
    EXAGGERATED_ADVERTISEMENT,

    /** 유사 서비스 존재 여부 검증 항목입니다. */
    SIMILAR_SERVICE,

    /** 마일스톤 구체성 검증 항목입니다. */
    MILESTONE_SPECIFICITY,

    /** AI 제공자 장애 항목입니다. */
    AI_PROVIDER_UNAVAILABLE,

    /** 재제출 제한 초과 항목입니다. */
    RESUBMISSION_LIMIT_EXCEEDED,

    /** 보완 기한 만료 항목입니다. */
    REVISION_DEADLINE_EXPIRED
}
