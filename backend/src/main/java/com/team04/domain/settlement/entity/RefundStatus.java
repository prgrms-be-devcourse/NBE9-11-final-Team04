package com.team04.domain.settlement.entity;

public enum RefundStatus {
    PENDING,    // 환불 대기 — PG 호출 전/후 처리 중
    COMPLETED,  // 환불 완료 — PG 취소 성공 및 장부 동기화 완료
    FAILED      // 환불 실패 — PG 취소 실패, 재시도 가능
}