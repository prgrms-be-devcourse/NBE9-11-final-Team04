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

    /** 요청한 상태의 삭제되지 않은 아이디어 심사 목록을 페이지로 조회합니다. */
    @Transactional(readOnly = true)
    public Page<AdminIdeaReviewResponse> getReviews(IdeaStatus status, Pageable pageable) {
        return ideaRepository.findByStatusInAndDeletedAtIsNull(List.of(status), pageable)
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

