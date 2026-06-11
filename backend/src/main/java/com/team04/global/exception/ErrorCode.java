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

    // 마일스톤
    MILESTONE_NOT_FOUND(404, "M001", "존재하지 않는 마일스톤입니다"),
    MILESTONE_ALREADY_APPROVED(400, "M002", "이미 승인된 마일스톤입니다"),

    // 결제
    PAYMENT_FAILED(400, "P001", "결제에 실패했습니다"),
    REFUND_FAILED(400, "P002", "환불 처리에 실패했습니다"),
    ESCROW_NOT_FOUND(404, "P003", "에스크로 정보를 찾을 수 없습니다"),

    // 분쟁
    DISPUTE_NOT_FOUND(404, "D001", "존재하지 않는 분쟁입니다"),
    DISPUTE_ALREADY_RESOLVED(400, "D002", "이미 처리된 분쟁입니다");

    private final int status;
    private final String code;
    private final String message;
}
