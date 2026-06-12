package com.team04.settlement.entity;

public final class SettlementTypes {

    private SettlementTypes() {
    }

    public enum SettlementType {
        FINAL,      // 최종 정산
        INTERIM     // 중간 정산
    }

    public enum SettlementStatus {
        PENDING,              // 정산 대기
        COMPLETED,            // 정산 완료
        PARTIALLY_REFUNDED,   // 소명 완료 후 부분 환불
        FORFEITED,            // 먹튀/잠수 보증금 몰수
        REFUNDED              // 목표 미달성 전액 환불
    }
}