package com.team04.domain.settlement.entity;

public enum RefundReason {
    GOAL_NOT_MET,    // 목표 미달성
    CANCELLED,       // 이행 중단
    DISPUTE,         // 분쟁
    SPONSOR_CANCEL   // 후원자 자발 취소
}