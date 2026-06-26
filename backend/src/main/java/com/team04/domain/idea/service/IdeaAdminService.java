package com.team04.domain.idea.service;

import com.team04.domain.idea.dto.response.AdminIdeaReviewResponse;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** 관리자 아이디어 심사 목록, 승인, 반려, 상태 통계 비즈니스 로직을 처리하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class IdeaAdminService {

    private final IdeaRepository ideaRepository;

    /** 요청한 상태의 삭제되지 않은 아이디어 심사 목록을 페이지로 조회합니다. status가 null이면 전체 조회합니다. */
    @Transactional(readOnly = true)
    public Page<AdminIdeaReviewResponse> getReviews(IdeaStatus status, Pageable pageable) {
        List<IdeaStatus> statuses = status != null ? List.of(status) : List.of(IdeaStatus.values());
        return ideaRepository.findByStatusInAndDeletedAtIsNull(statuses, pageable)
                .map(AdminIdeaReviewResponse::of);
    }

    /** 소유자 검증 없이 관리자 권한으로 아이디어를 공개 승인 상태로 전이합니다. */
    @Transactional
    public void approve(Long ideaId) {
        Idea idea = findActiveIdea(ideaId);
        if (idea.getStatus() != IdeaStatus.ADMIN_PENDING) {
            throw new CustomException(ErrorCode.INVALID_IDEA_STATUS_TRANSITION);
        }
        idea.open();
    }

    /** 소유자 검증 없이 관리자 권한으로 아이디어를 반려하고 반려 사유를 저장합니다. */
    @Transactional
    public void reject(Long ideaId, String reason) {
        Idea idea = findActiveIdea(ideaId);
        if (idea.getStatus() != IdeaStatus.ADMIN_PENDING) {
            throw new CustomException(ErrorCode.INVALID_IDEA_STATUS_TRANSITION);
        }
        idea.reject(reason);
    }

    /** 관리자 권한으로 OPEN 또는 IN_PROGRESS 상태의 아이디어를 일시 중단합니다. */
    @Transactional
    public void suspendIdea(Long ideaId) {
        Idea idea = findActiveIdea(ideaId);
        idea.suspend();
    }

    // 소명 수용(DisputeStatus.REJECTED) 시 DisputeService에서 호출됩니다.
    /** 일시 중단된 아이디어를 중단 전 상태로 복원합니다. SUSPENDED 상태가 아니면 무시합니다. */
    @Transactional
    public void restoreIdea(Long ideaId) {
        Idea idea = findActiveIdea(ideaId);
        if (idea.getStatus() == IdeaStatus.SUSPENDED) {
            idea.restore();
        }
    }

    // 신고 인정(DisputeStatus.RESOLVED) 시 DisputeService에서 호출됩니다.
    /** 분쟁 신고 인정으로 아이디어를 강제 취소합니다. 이미 종료된 상태면 무시합니다. */
    @Transactional
    public void cancelIdeaForDispute(Long ideaId) {
        Idea idea = findActiveIdea(ideaId);
        if (idea.getStatus() != IdeaStatus.COMPLETED
                && idea.getStatus() != IdeaStatus.CANCELLED
                && idea.getStatus() != IdeaStatus.REJECTED) {
            idea.changeStatus(IdeaStatus.CANCELLED);
        }
    }

    /** 전체 아이디어 상태별 건수를 반환합니다. */
    @Transactional(readOnly = true)
    public Map<IdeaStatus, Long> getStatusStats() {
        return ideaRepository.countByStatus();
    }

    /** 소프트 삭제되지 않은 아이디어를 조회하고 없으면 공통 예외를 발생시킵니다. */
    private Idea findActiveIdea(Long ideaId) {
        return ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
    }
}

