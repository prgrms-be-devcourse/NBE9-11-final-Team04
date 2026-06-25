package com.team04.domain.settlement.entity;

public enum SettlementStatus {
    PENDING,            // 정산 대기
    COMPLETED,          // 정산 완료 (최종 완성)
    PARTIALLY_REFUNDED, // 정당한 사유 중단 — 보증금 잔액 제안자 환급
    DEPOSIT_REFUNDED,   // 보증금 전액 제안자 환급
    DEPOSIT_EXHAUSTED,  // 정당한 사유 중단 — 선정산이 보증금 초과, 환급액 0
    FORFEITED,          // 단순 포기/먹튀 — 보증금 몰수
    REFUNDED,           // 후원자 환불 (목표 미달성 / 중단 / 포기)
    FAILED              // 정산/보증금 환급 지급 실패
}
