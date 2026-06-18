package com.team04.domain.milestone.entity;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;

/** 마일스톤 진행 상태 및 상태 전이 규칙을 관리하는 열거형입니다. */
public enum MilestoneStatus {

    /** 대기 상태입니다. */
    PENDING,

    /** 진행 중 상태입니다. */
    IN_PROGRESS,

    /** 완료 상태입니다. */
    COMPLETED,

    /** 취소 상태입니다. */
    CANCELLED;

    /** 현재 상태에서 목표 상태로 전이할 수 있는지 확인합니다. */
    public boolean canTransitionTo(MilestoneStatus targetStatus) {
        return switch (this) {
            case PENDING -> targetStatus == IN_PROGRESS || targetStatus == CANCELLED;
            case IN_PROGRESS -> targetStatus == COMPLETED || targetStatus == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    /** 유효하지 않은 상태 전이를 공통 예외로 차단합니다. */
    public void validateTransitionTo(MilestoneStatus targetStatus) {
        if (!canTransitionTo(targetStatus)) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
        }
    }
}