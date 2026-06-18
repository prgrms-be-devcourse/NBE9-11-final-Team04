package com.team04.domain.settlement.entity;

/** 선정산 처리 상태를 관리하는 열거형입니다. */
public enum PreSettlementStatus {

    /** 선정산 신청 완료 (즉시 지급 대기) */
    REQUESTED,

    /** 지급 완료 */
    COMPLETED,

    /** 지급 실패 */
    FAILED
}