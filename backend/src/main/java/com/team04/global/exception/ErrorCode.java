package com.team04.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(400, "C001", "잘못된 입력값입니다"),
    UNAUTHORIZED(401, "C002", "인증이 필요합니다"),
    FORBIDDEN(403, "C003", "접근 권한이 없습니다"),
    INTERNAL_SERVER_ERROR(500, "C004", "서버 내부 오류가 발생했습니다"),

    // 회원
    USER_NOT_FOUND(404, "U001", "존재하지 않는 사용자입니다"),
    DUPLICATE_EMAIL(409, "U002", "이미 사용 중인 이메일입니다"),
    INVALID_PASSWORD(400, "U003", "비밀번호가 올바르지 않습니다"),
    ACCOUNT_SUSPENDED(403, "U004", "정지된 계정입니다"),

    // 인증
    INVALID_REFRESH_TOKEN(401, "A001", "유효하지 않은 리프레시 토큰입니다"),
    INVALID_OTP(400, "A002", "인증 코드가 올바르지 않습니다"),
    OTP_EXPIRED(400, "A003", "인증 코드가 만료되었습니다"),
    ACCOUNT_WITHDRAWN(403, "U005", "탈퇴한 계정입니다"),

    // 아이디어
    IDEA_NOT_FOUND(404, "I001", "존재하지 않는 아이디어입니다"),
    IDEA_ACCESS_DENIED(403, "I002", "NDA 동의 후 열람 가능합니다"),
    IDEA_EMBARGOED(403, "I003", "엠바고 기간 중인 아이디어입니다"),

    // 전문가
    EXPERT_NOT_FOUND(404, "E001", "존재하지 않는 전문가입니다"),
    EXPERT_NOT_VERIFIED(403, "E002", "자격 검증이 완료되지 않은 전문가입니다"),
    EXPERT_SUSPENDED(403, "E003", "자격 정지된 전문가 계정입니다"),

    // 펀딩
    FUNDING_NOT_FOUND(404, "F001", "존재하지 않는 펀딩입니다"),
    FUNDING_ALREADY_CLOSED(400, "F002", "이미 마감된 펀딩입니다"),
    FUNDING_GOAL_NOT_MET(400, "F003", "목표 금액 미달성으로 환불 처리됩니다"),
    FUNDING_DUPLICATE_PAYMENT(409, "F004", "중복 결제 요청입니다"),
    IDEA_NOT_OPEN(400, "F005", "후원 가능한 상태의 프로젝트가 아닙니다"),
    IDEA_SELF_FUNDING_NOT_ALLOWED(400, "F006", "본인 프로젝트는 후원할 수 없습니다"),
    INVALID_FUNDING_AMOUNT(400, "F007", "후원 금액이 올바르지 않습니다"),
    PROJECT_FEE_NOT_FOUND(404, "F008", "존재하지 않는 프로젝트 수수료입니다"),
    PROJECT_FEE_ALREADY_PAID(409, "F009", "이미 수수료가 결제된 프로젝트입니다"),

    // 마일스톤
    MILESTONE_NOT_FOUND(404, "M001", "존재하지 않는 마일스톤입니다"),
    MILESTONE_ALREADY_APPROVED(400, "M002", "이미 승인된 마일스톤입니다"),

    // 결제
    PAYMENT_NOT_FOUND(404, "P001", "존재하지 않는 결제입니다"),
    PAYMENT_FAILED(400, "P002", "결제에 실패했습니다"),
    REFUND_FAILED(400, "P003", "환불 처리에 실패했습니다"),
    ESCROW_NOT_FOUND(404, "P004", "에스크로 정보를 찾을 수 없습니다"),
    PAYMENT_AMOUNT_MISMATCH(400, "P005", "결제 금액이 일치하지 않습니다"),
    PAYMENT_ALREADY_DONE(409, "P006", "이미 완료된 결제입니다"),
    PAYMENT_NOT_READY(400, "P007", "결제를 진행할 수 없는 상태입니다"),

    // 분쟁
    DISPUTE_NOT_FOUND(404, "D001", "존재하지 않는 분쟁입니다"),
    DISPUTE_ALREADY_RESOLVED(400, "D002", "이미 처리된 분쟁입니다");

    private final int status;
    private final String code;
    private final String message;
}
