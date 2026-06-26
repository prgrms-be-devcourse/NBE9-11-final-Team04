package com.team04.domain.payment.entity;

/** 가상계좌 장부의 입출금 방향입니다. */
public enum VbankLedgerDirection {
    /** 가상계좌 잔액이 증가하는 기록입니다. */
    IN,
    /** 가상계좌 잔액이 감소하거나, 공개용 출금 내역으로 표시되는 기록입니다. */
    OUT
}
