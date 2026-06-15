package com.team04.domain.idea.service;

import com.team04.domain.idea.dto.request.IdeaDraftRequest;
import com.team04.domain.idea.dto.response.IdeaDraftResponse;
import com.team04.domain.idea.dto.response.IdeaSummaryResponse;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.IdeaDraft;
import com.team04.domain.idea.repository.IdeaDraftRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.domain.idea.dto.request.CreateIdeaRequest;
import com.team04.domain.idea.dto.request.ReportIdeaRequest;
import com.team04.domain.idea.dto.request.UpdateIdeaRequest;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.dto.response.ReportIdeaResponse;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.event.IdeaCreatedEvent;
import com.team04.domain.idea.event.IdeaPlagiarismReportedEvent;
import com.team04.domain.idea.event.IdeaReportNotificationEvent;
import com.team04.domain.idea.repository.IdeaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 아이디어 등록, 조회, 수정, 삭제, 신고 비즈니스 로직을 처리하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class IdeaService {

    private static final int REQUIRED_MILESTONE_COUNT = 3;
    private static final int DRAFT_RETENTION_DAYS = 30;
    private static final int MAX_DRAFT_COUNT = 50;

    private final IdeaRepository ideaRepository;
    private final IdeaDraftRepository ideaDraftRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 아이디어를 등록하고 마일스톤 생성 이벤트를 발행합니다. */
    @Transactional
    public IdeaResponse createIdea(Long userId, CreateIdeaRequest request) {
        validateMilestones(request);

        Idea idea = new Idea(
                userId,
                request.title(),
                request.category(),
                request.oneLineIntro(),
                request.problemDefinition(),
                request.solution(),
                request.goal(),
                request.targetCustomer(),
                request.competitor(),
                request.teamIntro(),
                request.goalAmount(),
                request.fundingStartAt(),
                request.fundingEndAt(),
                request.rewardType()
        );
        Idea savedIdea = ideaRepository.save(idea);

        // 마일스톤 도메인 담당자가 핸들러 구현 후 처리
        eventPublisher.publishEvent(new IdeaCreatedEvent(savedIdea.getId(), request.milestones()));

        return IdeaResponse.of(savedIdea);
    }

    /** 프로젝트 목록을 카테고리, 마감임박 필터, 정렬 조건에 따라 Slice로 조회합니다. */
    @Transactional(readOnly = true)
    public Slice<IdeaSummaryResponse> getProjects(
            IdeaCategory category,
            Boolean closingSoonOnly,
            String sort,
            Pageable pageable
    ) {
        return ideaRepository.searchProjects(category, closingSoonOnly, null, sort, pageable)
                .map(IdeaSummaryResponse::of);
    }

    /** 프로젝트명을 기준으로 LIKE 검색하고 목록 조회와 동일한 Slice 응답 구조로 반환합니다. */
    @Transactional(readOnly = true)
    public Slice<IdeaSummaryResponse> searchProjects(String keyword, String sort, Pageable pageable) {
        return ideaRepository.searchProjects(null, false, keyword, sort, pageable)
                .map(IdeaSummaryResponse::of);
    }

    /** 보관 기간 내 본인 임시저장 목록을 최신 수정순으로 조회합니다. */
    @Transactional(readOnly = true)
    public List<IdeaDraftResponse> getDrafts(Long userId) {
        return ideaDraftRepository.findByUserIdAndUpdatedAtAfterOrderByUpdatedAtDesc(userId, draftRetentionStartAt())
                .stream()
                .map(IdeaDraftResponse::of)
                .toList();
    }

    /** 최대 개수 제한을 검증한 뒤 로그인 사용자의 아이디어 임시저장을 생성합니다. */
    @Transactional
    public IdeaDraftResponse createDraft(Long userId, IdeaDraftRequest request) {
        validateDraftLimit(userId);
        IdeaDraft draft = new IdeaDraft(userId);
        draft.update(
                request.title(),
                request.category(),
                request.oneLineIntro(),
                request.problemDefinition(),
                request.solution(),
                request.goal(),
                request.targetCustomer(),
                request.competitor(),
                request.teamIntro(),
                request.goalAmount(),
                request.fundingStartAt(),
                request.fundingEndAt(),
                request.rewardType()
        );
        return IdeaDraftResponse.of(ideaDraftRepository.save(draft));
    }

    /** 본인 임시저장만 수정할 수 있도록 검증한 뒤 내용을 갱신합니다. */
    @Transactional
    public IdeaDraftResponse updateDraft(Long draftId, Long userId, IdeaDraftRequest request) {
        IdeaDraft draft = findDraft(draftId, userId);
        draft.update(
                request.title(),
                request.category(),
                request.oneLineIntro(),
                request.problemDefinition(),
                request.solution(),
                request.goal(),
                request.targetCustomer(),
                request.competitor(),
                request.teamIntro(),
                request.goalAmount(),
                request.fundingStartAt(),
                request.fundingEndAt(),
                request.rewardType()
        );
        return IdeaDraftResponse.of(draft);
    }

    /** 본인 임시저장만 삭제할 수 있도록 검증한 뒤 임시저장을 제거합니다. */
    @Transactional
    public void deleteDraft(Long draftId, Long userId) {
        IdeaDraft draft = findDraft(draftId, userId);
        ideaDraftRepository.delete(draft);
    }

    /** 본인 임시저장 내용으로 아이디어를 정식 등록하고 성공 시 임시저장을 삭제합니다. */
    @Transactional
    public IdeaResponse publishDraft(Long draftId, Long userId, CreateIdeaRequest request) {
        IdeaDraft draft = findDraft(draftId, userId);
        IdeaResponse response = createIdea(userId, request);
        ideaDraftRepository.delete(draft);
        return response;
    }

    /** 삭제되지 않은 아이디어 상세 정보를 조회합니다. */
    @Transactional(readOnly = true)
    public IdeaResponse getIdea(Long ideaId) {
        Idea idea = findActiveIdea(ideaId);
        return IdeaResponse.of(idea);
    }

    /** 작성자 본인이고 심사 대기 상태인 경우에만 아이디어 정보를 수정합니다. */
    @Transactional
    public IdeaResponse updateIdea(Long ideaId, Long userId, UpdateIdeaRequest request) {
        Idea idea = findActiveIdea(ideaId);
        idea.validateOwner(userId);
        idea.update(
                request.title(),
                request.category(),
                request.oneLineIntro(),
                request.problemDefinition(),
                request.solution(),
                request.goal(),
                request.targetCustomer(),
                request.competitor(),
                request.teamIntro(),
                request.goalAmount(),
                request.fundingStartAt(),
                request.fundingEndAt(),
                request.rewardType()
        );
        return IdeaResponse.of(idea);
    }

    /** 작성자 본인이고 심사 대기 상태인 경우에만 아이디어를 소프트 삭제합니다. */
    @Transactional
    public void deleteIdea(Long ideaId, Long userId) {
        Idea idea = findActiveIdea(ideaId);
        idea.validateOwner(userId);
        idea.softDelete();
    }

    /** 아이디어 도용 신고 이벤트와 관리자 알림 이벤트를 발행합니다. */
    @Transactional
    public ReportIdeaResponse reportIdea(Long ideaId, Long reporterUserId, ReportIdeaRequest request) {
        Idea idea = findActiveIdea(ideaId);
        if (idea.getUserId().equals(reporterUserId)) {
            throw new CustomException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
        }
        eventPublisher.publishEvent(
                new IdeaPlagiarismReportedEvent(idea.getId(), idea.getUserId(), reporterUserId, request.reason())
        );
        eventPublisher.publishEvent(
                new IdeaReportNotificationEvent(idea.getId(), reporterUserId, request.reason())
        );
        return new ReportIdeaResponse(idea.getId(), reporterUserId, "아이디어 도용 신고가 접수되었습니다.");
    }

    /** 임시저장 보관 기준 시각을 계산합니다. */
    private LocalDateTime draftRetentionStartAt() {
        return LocalDateTime.now().minusDays(DRAFT_RETENTION_DAYS);
    }

    /** 보관 기간 내 임시저장이 최대 50개를 넘지 않는지 검증합니다. */
    private void validateDraftLimit(Long userId) {
        if (ideaDraftRepository.countByUserIdAndUpdatedAtAfter(userId, draftRetentionStartAt()) >= MAX_DRAFT_COUNT) {
            throw new CustomException(ErrorCode.IDEA_DRAFT_LIMIT_EXCEEDED);
        }
    }

    /** 임시저장을 조회하고 작성자 본인 접근인지 검증합니다. */
    private IdeaDraft findDraft(Long draftId, Long userId) {
        IdeaDraft draft = ideaDraftRepository.findById(draftId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_DRAFT_NOT_FOUND));
        draft.validateOwner(userId);
        if (draft.getCreatedAt().isBefore(draftRetentionStartAt())) {
            throw new CustomException(ErrorCode.IDEA_DRAFT_NOT_FOUND);
        }
        return draft;
    }

    /** 소프트 삭제되지 않은 아이디어를 조회하고 없으면 공통 예외를 발생시킵니다. */
    private Idea findActiveIdea(Long ideaId) {
        return ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
    }

    /** 마일스톤 개수와 단계 값이 정확히 1, 2, 3인지 검증합니다. */
    private void validateMilestones(CreateIdeaRequest request) {
        if (request.milestones() == null || request.milestones().size() != REQUIRED_MILESTONE_COUNT) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_COUNT);
        }

        Set<Integer> steps = new HashSet<>();
        for (var milestone : request.milestones()) {
            steps.add(milestone.step());
        }

        if (!steps.containsAll(List.of(1, 2, 3)) || steps.size() != REQUIRED_MILESTONE_COUNT) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STEP);
        }
    }
}