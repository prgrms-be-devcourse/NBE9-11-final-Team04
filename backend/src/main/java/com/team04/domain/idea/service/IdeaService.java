package com.team04.domain.idea.service;

import com.team04.domain.dispute.dto.request.CreateDisputeRequest;
import com.team04.domain.dispute.entity.DisputeCategory;
import com.team04.domain.dispute.entity.TargetType;
import com.team04.domain.dispute.service.DisputeService;
import com.team04.domain.idea.dto.request.IdeaDraftRequest;
import com.team04.domain.idea.dto.response.*;
import com.team04.domain.idea.entity.*;
import com.team04.domain.idea.repository.IdeaBookmarkRepository;
import com.team04.domain.idea.repository.IdeaDraftRepository;
import com.team04.domain.milestone.dto.response.MilestoneResponse;
import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.domain.idea.dto.request.CreateIdeaRequest;
import com.team04.domain.idea.dto.request.ReportIdeaRequest;
import com.team04.domain.idea.dto.request.UpdateIdeaRequest;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.global.storage.StorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final IdeaBookmarkRepository ideaBookmarkRepository;
    private final MilestoneRepository milestoneRepository;
    private final DisputeService disputeService;
    private final StorageClient storageClient;

    /** 아이디어를 등록하고 마일스톤을 함께 저장합니다. */
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
                request.depositAmount(),
                request.fundingStartAt(),
                request.fundingEndAt(),
                request.rewardType(),
                request.imageUrl()
        );
        Idea savedIdea = ideaRepository.save(idea);

        milestoneRepository.saveAll(
                request.milestones().stream()
                        .map(m -> Milestone.builder()
                                .ideaId(savedIdea.getId())
                                .step(m.step())
                                .goal(m.goal())
                                .expectedResult(m.expectedResult())
                                .expectedDate(m.expectedDate())
                                .build())
                        .toList()
        );

        return IdeaResponse.of(savedIdea);
    }

    /** 프로젝트 목록을 카테고리, 마감임박 필터, 정렬 조건에 따라 Slice로 조회합니다. */
    @Transactional(readOnly = true)
    public Slice<IdeaSummaryResponse> getProjects(
            IdeaCategory category,
            Boolean closingSoonOnly,
            String keyword,
            String sort,
            Pageable pageable
    ) {
        return ideaRepository.searchProjects(category, closingSoonOnly, keyword, sort, pageable)
                .map(IdeaSummaryResponse::of);
    }

    /** 신뢰도와 펀딩 달성률, 후원자 수를 합산한 인기 프로젝트 Top5를 조회합니다. */
    @Transactional(readOnly = true)
    public List<IdeaResponse> getTop5Ideas() {
        return ideaRepository.findTop5PopularIdeas()
                .stream()
                .map(IdeaResponse::of)
                .toList();
    }

    /** 진행 중인 본인 아이디어를 취소 신청 상태로 전이합니다. */
    @Transactional
    public void requestCancellation(Long ideaId, Long userId) {
        Idea idea = findActiveIdea(ideaId);
        idea.validateOwner(userId);
        idea.requestCancellation();
    }

    /** 중복 여부를 확인한 뒤 로그인 사용자의 관심 프로젝트를 저장합니다. */
    @Transactional
    public void addBookmark(Long ideaId, Long userId) {
        findActiveIdea(ideaId);
        if (ideaBookmarkRepository.existsByUserIdAndIdeaId(userId, ideaId)) {
            throw new CustomException(ErrorCode.IDEA_BOOKMARK_ALREADY_EXISTS);
        }
        ideaBookmarkRepository.save(new IdeaBookmark(userId, ideaId));
    }

    /** 존재 여부를 확인한 뒤 로그인 사용자의 관심 프로젝트를 삭제합니다. */
    @Transactional
    public void deleteBookmark(Long ideaId, Long userId) {
        int deleted = ideaBookmarkRepository.deleteByUserIdAndIdeaIdBulk(userId, ideaId);
        if (deleted == 0) {
            throw new CustomException(ErrorCode.IDEA_BOOKMARK_NOT_FOUND);
        }
    }

    /** 로그인 사용자의 관심 프로젝트 목록을 Slice 페이지네이션으로 조회합니다. */
    @Transactional(readOnly = true)
    public Slice<IdeaResponse> getBookmarks(Long userId, Pageable pageable) {
        return ideaBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(bookmark -> IdeaResponse.of(findActiveIdea(bookmark.getIdeaId())));
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
                request.depositAmount(),
                request.fundingStartAt(),
                request.fundingEndAt(),
                request.rewardType(),
                request.imageUrl()
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
                request.depositAmount(),
                request.fundingStartAt(),
                request.fundingEndAt(),
                request.rewardType(),
                request.imageUrl()
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

    /** 로그인 사용자가 등록한 아이디어 목록을 조회합니다. */
    @Transactional(readOnly = true)
    public List<IdeaSummaryResponse> getMyIdeas(Long userId) {
        return ideaRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(IdeaSummaryResponse::of)
                .toList();
    }

    /** 삭제되지 않은 아이디어 상세 정보를 조회합니다. */
    @Transactional(readOnly = true)
    public IdeaResponse getIdea(Long ideaId) {
        Idea idea = findActiveIdea(ideaId);
        List<MilestoneResponse> milestones = milestoneRepository.findByIdeaIdOrderByStep(ideaId)
                .stream()
                .map(MilestoneResponse::from)
                .toList();
        return IdeaResponse.of(idea, milestones);
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
                request.rewardType(),
                request.imageUrl()
        );
        return IdeaResponse.of(idea);
    }

    /** 작성자 본인이고 심사 대기 상태인 경우에만 대표 이미지를 업로드하고 URL을 저장합니다. */
    @Transactional
    public IdeaResponse uploadIdeaImage(Long ideaId, Long userId, MultipartFile image) {
        Idea idea = findActiveIdea(ideaId);
        idea.validateOwner(userId);
        validateImageFile(image);

        String imageUrl = storageClient.upload(image, "idea/image");
        idea.updateImageUrl(imageUrl);

        return IdeaResponse.of(idea);
    }

    /** 작성자 본인이고 심사 대기 상태인 경우에만 아이디어를 소프트 삭제합니다. */
    @Transactional
    public void deleteIdea(Long ideaId, Long userId) {
        Idea idea = findActiveIdea(ideaId);
        idea.validateOwner(userId);
        idea.softDelete();
    }

    /** 아이디어 도용 신고를 접수하고 분쟁을 생성합니다. */
    @Transactional
    public ReportIdeaResponse reportIdea(Long ideaId, Long reporterUserId, ReportIdeaRequest request) {
        Idea idea = findActiveIdea(ideaId);
        if (idea.getUserId().equals(reporterUserId)) {
            throw new CustomException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
        }
        disputeService.createDispute(reporterUserId,
                new CreateDisputeRequest(
                        TargetType.IDEA,
                        ideaId,
                        idea.getUserId(),
                        DisputeCategory.IDEA_THEFT,
                        "아이디어 도용 신고",
                        request.reason(),
                        null
                ));
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

    /** 업로드할 이미지 파일이 비어 있지 않은지 검증합니다. */
    private void validateImageFile(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
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
    /** 펀딩 마감됐고 목표 금액 미달성인 아이디어 ID 목록을 반환합니다. */
    @Transactional(readOnly = true)
    public List<Long> getFailedFundingIdeaIds() {
        return ideaRepository.findFailedFundingIdeaIds(LocalDateTime.now());
    }
}