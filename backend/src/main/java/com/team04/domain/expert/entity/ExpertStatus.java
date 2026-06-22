package com.team04.domain.expert.entity;

public enum ExpertStatus {
    ACTIVE,     // 정상
    SUSPENDED,   // 격리
    PENDING_VERIFICATION ,   // 검증 보류
    DEMOTED               // 권한 강등 (SPONSOR로 변경된 전문가)
}
