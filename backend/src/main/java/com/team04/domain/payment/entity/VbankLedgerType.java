package com.team04.domain.payment.entity;

/** 아이디어별 가상계좌 장부에 기록되는 자금 흐름 유형입니다. */
public enum VbankLedgerType {
    /** 제안자가 보증금을 납부한 기록입니다. */
    DEPOSIT_PAID,
    /** 후원자가 후원금을 납부한 기록입니다. */
    FUNDING_PAID,
    /** 제안자에게 선정산 금액을 지급한 기록입니다. */
    PRE_SETTLEMENT_PAID,
    /** 후원자에게 환불 금액을 지급한 기록입니다. */
    SPONSOR_REFUND_PAID,
    /** 제안자에게 보증금을 환급한 기록입니다. */
    DEPOSIT_REFUNDED,
    /** 보증금을 몰수 처리한 공개용 기록입니다. */
    DEPOSIT_FORFEITED,
    /** 제안자에게 최종 정산금을 지급한 기록입니다. */
    FINAL_SETTLEMENT_PAID,
    /** 제안자가 자금 사용 내역을 등록한 공개용 기록입니다. */
    FUND_USAGE_RECORDED
}
