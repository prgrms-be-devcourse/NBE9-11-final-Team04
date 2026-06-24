package com.team04.domain.idea.entity;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;

/** 아이디어 심사와 펀딩 진행 상태 및 상태 전이 규칙을 관리하는 열거형입니다. */
public enum IdeaStatus {

    /** AI 심사 대기 상태입니다. */
    AI_PENDING,

    /** 전문가 심사 대기 상태입니다. */
    EXPERT_PENDING,

    /** 관리자 심사 대기 상태입니다. */
    ADMIN_PENDING,

    /** 심사 또는 검증에서 반려된 상태입니다. */
    REJECTED,

    /** 펀딩 공개 전환이 완료된 상태입니다. */
    OPEN,

    /** 펀딩이 진행 중인 상태입니다. */
    IN_PROGRESS,

    /** 펀딩 또는 아이디어 진행이 완료된 상태입니다. */
    COMPLETED,

    /** 펀딩 오픈 이후 제안자가 취소를 신청한 상태입니다. */
    CANCELLATION_REQUESTED,

    /** 아이디어 진행이 취소된 상태입니다. */
    CANCELLED,

    /** 관리자가 일시 중단한 상태입니다. */
    SUSPENDED;

    /** 아이디어 내용을 수정할 수 있는 심사 대기 상태인지 확인합니다. */
    public boolean isEditable() {
        return this == AI_PENDING || this == EXPERT_PENDING || this == ADMIN_PENDING;
    }

    /** 아이디어를 삭제할 수 있는 심사 대기 상태인지 확인합니다. */
    public boolean isDeletable() {
        return isEditable();
    }

    /** 현재 상태에서 목표 상태로 전이할 수 있는지 확인합니다. */
    public boolean canTransitionTo(IdeaStatus targetStatus) {
        return switch (this) {
            case AI_PENDING ->
                    targetStatus == EXPERT_PENDING
                            || targetStatus == CANCELLED;

            case EXPERT_PENDING ->
                    targetStatus == ADMIN_PENDING
                            || targetStatus == CANCELLED;

            case ADMIN_PENDING ->
                    targetStatus == OPEN
                            || targetStatus == REJECTED
                            || targetStatus == CANCELLED;

            case OPEN ->
                    targetStatus == IN_PROGRESS
                            || targetStatus == SUSPENDED
                            || targetStatus == CANCELLED;

            case IN_PROGRESS ->
                    targetStatus == COMPLETED
                            || targetStatus == CANCELLATION_REQUESTED
                            || targetStatus == SUSPENDED
                            || targetStatus == CANCELLED;

            case CANCELLATION_REQUESTED ->
                    targetStatus == CANCELLED
                            || targetStatus == IN_PROGRESS;

            case SUSPENDED ->
                    targetStatus == OPEN
                            || targetStatus == IN_PROGRESS;

            case COMPLETED, CANCELLED, REJECTED ->
                    false;
        };
    }

    /** 유효하지 않은 상태 전이를 공통 예외로 차단합니다. */
    public void validateTransitionTo(IdeaStatus targetStatus) {
        if (!canTransitionTo(targetStatus)) {
            throw new CustomException(ErrorCode.INVALID_IDEA_STATUS_TRANSITION);
        }
    }
}