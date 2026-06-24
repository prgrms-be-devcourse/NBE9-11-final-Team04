package com.team04.domain.verification.entity;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;

/** 프로젝트 검증 요청의 진행 상태와 상태 전이 규칙을 관리하는 열거형입니다. */
public enum VerificationStatus {

    /** 검증 요청이 임시 작성 중인 상태입니다. */
    DRAFT,

    /** AI 검증이 진행 중인 상태입니다. */
    AI_VERIFYING,

    /** AI 검증을 통과한 상태입니다. */
    AI_PASSED,

    /** 보완 후 재제출이 필요한 상태입니다. */
    NEEDS_REVISION,

    /** 검증 요청이 반려된 상태입니다. */
    REJECTED,

    /** 관리자 검토를 기다리는 상태입니다. */
    PENDING_ADMIN_REVIEW,

    /** 전문가 매칭이 진행 중인 상태입니다. */
    EXPERT_MATCHING,

    /** 아이디어 취소 신청으로 검증이 중단된 상태입니다. */
    CANCELLED;

    /** 현재 상태에서 목표 상태로 전이할 수 있는지 확인합니다. */
    public boolean canTransitionTo(VerificationStatus targetStatus) {
        return switch (this) {
            case DRAFT -> targetStatus == AI_VERIFYING;
            case AI_VERIFYING -> targetStatus == AI_PASSED
                    || targetStatus == NEEDS_REVISION
                    || targetStatus == REJECTED
                    || targetStatus == PENDING_ADMIN_REVIEW
                    || targetStatus == CANCELLED;
            case AI_PASSED -> targetStatus == EXPERT_MATCHING
                    || targetStatus == PENDING_ADMIN_REVIEW;
            case NEEDS_REVISION -> targetStatus == AI_VERIFYING
                    || targetStatus == REJECTED;
            case PENDING_ADMIN_REVIEW -> targetStatus == EXPERT_MATCHING
                    || targetStatus == NEEDS_REVISION
                    || targetStatus == REJECTED;
            case EXPERT_MATCHING, REJECTED -> false;
            case CANCELLED -> false;
        };
    }

    /** 목표 상태로의 전이가 유효하지 않으면 검증 도메인 예외를 발생시킵니다. */
    public void validateTransitionTo(VerificationStatus targetStatus) {
        if (!canTransitionTo(targetStatus)) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_STATUS_TRANSITION);
        }
    }
}