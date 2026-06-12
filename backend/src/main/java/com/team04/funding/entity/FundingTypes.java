package com.team04.funding.entity;

public final class FundingTypes {

    private FundingTypes() {
    }

    public enum FundingStatus {
        PENDING_PAYMENT,  // 결제대기 — 후원 생성 후 결제 전
        PAID,             // 완료 — 결제 승인 완료
        REFUNDED          // 환불 — 환불 처리 완료
    }

    public enum DepositStatus {
        HELD,       // 예치 중 — 보증금 납부 완료, 프로젝트 진행 중
        REFUNDED,   // 환급 — 조건 충족 시 창작자에게 반환
        FORFEITED   // 몰수 — 프로젝트 실패·위반 시 몰수
    }
}
