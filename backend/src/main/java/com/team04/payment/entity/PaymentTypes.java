package com.team04.payment.entity;

public final class PaymentTypes {

    private PaymentTypes() {
    }

    public enum PaymentMethod {
        CARD,             // 카드 결제
        VIRTUAL_ACCOUNT   // 가상계좌 입금
    }

    public enum PaymentStatus {
        PENDING,   // 대기 — 결제 요청 생성, 승인 전
        SUCCESS,   // 성공 — PG 승인 완료
        FAILED,    // 실패 — PG 승인 실패 또는 사용자 이탈
        REFUNDED   // 환불 — PG 환불 완료
    }

    public enum VbankDepositStatus {
        WAITING,   // 입금 대기 — 가상계좌 발급, 입금 전
        DONE,      // 입금 완료 — 웹훅으로 입금 확인
        CANCELED,  // 취소 — 결제 취소로 입금 불가
        EXPIRED    // 만료 — 입금 기한 초과
    }
}
