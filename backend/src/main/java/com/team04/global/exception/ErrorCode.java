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
    UNDERAGE(403, "U006", "만 19세 미만은 가입할 수 없습니다"),

    // 사업자검증
    BUSINESS_VERIFICATION_UNAVAILABLE(503, "B001", "사업자 인증 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요"),
    BUSINESS_VERIFICATION_FAILED(400, "B002", "사업자 진위확인에 실패했습니다"),
    BUSINESS_ALREADY_REGISTERED(409, "B003", "이미 등록된 사업자번호입니다"),
    BUSINESS_REGISTRATION_NOT_FOUND(404, "B004", "사업자 인증 정보가 존재하지 않습니다"),

    // 아이디어
    IDEA_NOT_FOUND(404, "I001", "존재하지 않는 아이디어입니다"),
    IDEA_STATUS_NOT_EDITABLE(400, "I004", "현재 상태에서는 아이디어를 수정할 수 없습니다"),
    IDEA_STATUS_NOT_DELETABLE(400, "I005", "현재 상태에서는 아이디어를 삭제할 수 없습니다"),
    INVALID_IDEA_STATUS_TRANSITION(400, "I006", "유효하지 않은 아이디어 상태 전이입니다"),
    IDEA_DRAFT_NOT_FOUND(404, "I007", "존재하지 않는 아이디어 임시저장입니다"),
    IDEA_DRAFT_LIMIT_EXCEEDED(409, "I008", "아이디어 임시저장은 최대 50개까지 가능합니다"),
    SELF_REPORT_NOT_ALLOWED(400, "I009", "본인 아이디어는 신고할 수 없습니다"),

    // 검증
    INVALID_VERIFICATION_STATUS_TRANSITION(400, "V001", "유효하지 않은 검증 상태 전이입니다"),
    VERIFICATION_NOT_FOUND(404, "V002", "존재하지 않는 검증 요청입니다"),
    VERIFICATION_WAITING_PERIOD_ACTIVE(409, "V003", "재등록 대기 기간이 만료되지 않았습니다"),
    USE_RESUBMIT_API(400, "V004", "보완 필요 상태에서는 재제출 API를 사용해야 합니다"),

    // AI 검증
    AI_RESPONSE_EMPTY(500, "V005", "OpenAI 응답이 비어 있습니다"),
    VERIFICATION_ALREADY_IN_PROGRESS(409, "V006", "이미 검증이 진행 중입니다"),

    // 전문가
    EXPERT_NOT_FOUND(404, "E001", "존재하지 않는 전문가입니다"),
    EXPERT_NOT_VERIFIED(403, "E002", "자격 검증이 완료되지 않은 전문가입니다"),
    EXPERT_SUSPENDED(403, "E003", "자격 정지된 전문가 계정입니다"),
    DUPLICATE_EXPERT_PROFILE(409, "E004", "이미 등록된 전문가 프로필입니다"),
    EXTERNAL_API_FAILURE(503, "E005", "외부 API 장애가 발생했습니다"),
    EXTERNAL_API_INVALID(400, "E006", "유효하지 않은 자격 정보입니다"),

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
    MILESTONE_ALREADY_COMPLETED(400, "M002", "이미 완료된 마일스톤입니다"),
    INVALID_MILESTONE_COUNT(400, "M003", "현재 상태에서 해당 상태로 전이할 수 없습니다"),
    INVALID_MILESTONE_STEP(400, "M004", "유효하지 않은 마일스톤 단계입니다"),
    INVALID_MILESTONE_STATUS_TRANSITION(400, "M005", "현재 상태에서 해당 상태로 전이할 수 없습니다"),

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
    DISPUTE_ALREADY_RESOLVED(400, "D002", "이미 처리된 분쟁입니다"),

    //정산
    SETTLEMENT_NOT_FOUND(404, "S001", "존재하지 않는 정산입니다"),
    SETTLEMENT_ALREADY_COMPLETED(409, "S002", "이미 완료된 정산입니다"),
    SETTLEMENT_DUPLICATE(409, "S003", "중복 정산 요청입니다"),
    SETTLEMENT_INVALID_STATUS(400, "S004", "정산 처리가 불가능한 상태입니다"),
    SETTLEMENT_ACCESS_DENIED(403, "S005", "정산 조회 권한이 없습니다"),
    SETTLEMENT_INVALID_STATUS_TRANSITION(400, "S006", "현재 상태에서 해당 상태로 전이할 수 없습니다"),

    // 선정산
    PRE_SETTLEMENT_LIMIT_EXCEEDED(400, "PS001", "선정산 신청 금액이 보증금 2배 한도를 초과합니다"),
    PRE_SETTLEMENT_INVALID_AMOUNT(400, "PS002", "선정산 신청 금액은 0보다 커야 합니다"),
    PRE_SETTLEMENT_MILESTONE_NOT_IN_PROGRESS(400, "PS003", "진행 중인 마일스톤에서만 선정산 신청이 가능합니다"),
    PRE_SETTLEMENT_NOT_FOUND(404, "PS004", "존재하지 않는 선정산입니다"),
    PRE_SETTLEMENT_REQUEST_FAILED(500, "PS005", "선정산 신청에 실패했습니다. 잠시 후 다시 시도해주세요"),

    //알림
    NOTIFICATION_NOT_FOUND(404, "N001", "존재하지 않는 알림입니다.");

    // 자금 사용 내역
    FUND_USAGE_NOT_FOUND(404, "FU001", "존재하지 않는 자금 사용 내역입니다"),
    FUND_USAGE_INVALID_AMOUNT(400, "FU002", "자금 사용 금액은 0보다 커야 합니다"),
    FUND_USAGE_NO_IN_PROGRESS_MILESTONE(400, "FU003", "진행 중인 마일스톤이 없어 자금 사용 내역을 입력할 수 없습니다"),
    FUND_USAGE_EXCEEDS_RECEIVED(400, "FU004", "실제 지급받은 금액을 초과하는 지출은 등록할 수 없습니다"),
    FUND_USAGE_INVALID_DATE(400, "FU005", "자금 사용일은 펀딩 시작일 이후여야 합니다");
    private final int status;
    private final String code;
    private final String message;
}
