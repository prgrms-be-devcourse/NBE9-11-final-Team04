package com.team04.domain.idea.service;

import com.team04.domain.dispute.dto.request.CreateDisputeRequest;
import com.team04.domain.dispute.entity.DisputeCategory;
import com.team04.domain.dispute.entity.TargetType;
import com.team04.domain.dispute.service.DisputeService;
import com.team04.domain.idea.dto.request.*;
import com.team04.domain.idea.dto.response.*;
import com.team04.domain.idea.entity.*;
import com.team04.domain.idea.repository.IdeaBookmarkRepository;
import com.team04.domain.idea.repository.IdeaDraftRepository;
import com.team04.domain.idea.repository.IdeaSettlementAccountRepository;
import com.team04.domain.match.repository.ExpertMatchRepository;
import com.team04.domain.milestone.dto.response.MilestoneResponse;
import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.domain.user.entity.Role;
import com.team04.domain.verification.dto.request.VerificationRequest;
import com.team04.domain.verification.entity.VerificationStatus;
import com.team04.domain.verification.repository.ProjectVerificationRepository;
import com.team04.domain.verification.service.VerificationService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.global.storage.StorageClient;
import com.team04.global.util.IdeaDraftMilestoneConverter;
import com.team04.global.util.ImageUrlConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** 아이디어 등록, 조회, 수정, 삭제, 신고 비즈니스 로직을 처리하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class IdeaService {

    private static final int REQUIRED_MILESTONE_COUNT = 3;
    private static final int DRAFT_RETENTION_DAYS = 30;
    private static final int MAX_DRAFT_COUNT = 50;

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_MIME_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final int MAX_CONTENT_IMAGE_COUNT = 10;

    private final IdeaRepository ideaRepository;
    private final IdeaDraftRepository ideaDraftRepository;
    private final IdeaBookmarkRepository ideaBookmarkRepository;
    private final MilestoneRepository milestoneRepository;
    private final DisputeService disputeService;
    private final StorageClient storageClient;
    private final VerificationService verificationService;
    private final ProjectVerificationRepository projectVerificationRepository;
    private final IdeaSettlementAccountRepository ideaSettlementAccountRepository;
    private final ExpertMatchRepository expertMatchRepository;
    private final IdeaDraftMilestoneConverter ideaDraftMilestoneConverter;

    /** 아이디어를 등록하고 마일스톤을 함께 저장합니다. */
    @Transactional
    public IdeaResponse createIdea(Long userId, CreateIdeaRequest request) {
        validateMilestones(request);
        validateDepositAmount(request.depositAmount(), request.goalAmount());

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
                request.imageUrl(),
                ImageUrlConverter.join(request.imageUrls())
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

        verificationService.requestVerification(
                new VerificationRequest(
                        savedIdea.getId(),
                        request.title(),
                        buildVerificationDescription(request),
                        request.milestones().stream()
                                .map(m -> new VerificationRequest.MilestoneInfo(
                                        m.goal(),
                                        m.expectedResult(),
                                        m.expectedDate(),
                                        null
                                ))
                                .toList()
                ),
                userId
        );

        return IdeaResponse.of(savedIdea);
    }

    private void validateDepositAmount(Long depositAmount, Long goalAmount) {
        if (depositAmount * 10 > goalAmount * 3) {
            throw new CustomException(ErrorCode.INVALID_DEPOSIT_AMOUNT);
        }
    }

    /** AI 검증에 전달할 아이디어 상세 설명을 생성합니다. */
    private String buildVerificationDescription(CreateIdeaRequest request) {
        return Stream.of(
                        request.oneLineIntro(),
                        request.problemDefinition(),
                        request.solution(),
                        request.goal(),
                        request.targetCustomer(),
                        request.competitor(),
                        request.teamIntro()
                )
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }

    /** 프로젝트 목록을 카테고리, 마감임박 필터, 정렬 조건에 따라 Page로 조회합니다. */
    @Transactional(readOnly = true)
    public Page<IdeaSummaryResponse> getProjects(
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
        projectVerificationRepository.findByIdeaId(ideaId)
                .filter(v -> v.getStatus() != VerificationStatus.CANCELLED)
                .ifPresent(v -> v.changeStatus(VerificationStatus.CANCELLED));
    }

    /** 중복 여부를 확인한 뒤 로그인 사용자의 관심 프로젝트를 저장합니다.
     * OPEN/IN_PROGRESS인 아이디어만 북마크가 가능합니다.
     * 북마크를 이미 한 상태에서 중지되거나 종료됐어도 확인이 가능합니다.*/
    @Transactional
    public void addBookmark(Long ideaId, Long userId) {
        Idea idea = findActiveIdea(ideaId);
        if (idea.getStatus() != IdeaStatus.OPEN
                && idea.getStatus() != IdeaStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.IDEA_STATUS_NOT_BOOKMARKABLE);
        }
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

    /** 로그인 사용자의 관심 프로젝트 목록을 Page 페이지네이션으로 조회합니다. */
    @Transactional(readOnly = true)
    public Page<IdeaResponse> getBookmarks(Long userId, Pageable pageable) {
        Page<IdeaBookmark> bookmarks = ideaBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<Long> ideaIds = bookmarks.stream()
                .map(IdeaBookmark::getIdeaId)
                .toList();
        if (ideaIds.isEmpty()) {
            return Page.empty(pageable);
        }
        Map<Long, Idea> ideasById = ideaRepository.findByIdInAndDeletedAtIsNull(ideaIds)
                .stream()
                .collect(Collectors.toMap(Idea::getId, Function.identity()));

        return bookmarks.map(bookmark -> {
            Idea idea = ideasById.get(bookmark.getIdeaId());
            if (idea == null) {
                throw new CustomException(ErrorCode.IDEA_NOT_FOUND);
            }
            return IdeaResponse.of(idea);
        });
    }

    /** 보관 기간 내 본인 임시저장 목록을 최신 수정순으로 조회합니다. */
    @Transactional(readOnly = true)
    public List<IdeaDraftResponse> getDrafts(Long userId) {
        return ideaDraftRepository.findByUserIdAndUpdatedAtAfterOrderByUpdatedAtDesc(userId, draftRetentionStartAt())
                .stream()
                .map(draft -> IdeaDraftResponse.of(draft, ideaDraftMilestoneConverter))
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
        draft.updateImageUrls(ImageUrlConverter.join(request.imageUrls()));
        draft.updateMilestones(ideaDraftMilestoneConverter.join(request.milestones()));
        return IdeaDraftResponse.of(ideaDraftRepository.save(draft), ideaDraftMilestoneConverter);
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
        draft.updateImageUrls(ImageUrlConverter.join(request.imageUrls()));
        draft.updateMilestones(ideaDraftMilestoneConverter.join(request.milestones()));
        return IdeaDraftResponse.of(draft, ideaDraftMilestoneConverter);
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

    /** 내부 서비스 호출용 아이디어 상세 조회 (권한 검사 없음) */
    /** 아이디어 상태와 요청자 권한에 따라 상세 정보를 반환합니다.
     * OPEN 이전은 작성자/전문가/관리자만, OPEN 이후는 모든 사용자가 조회 가능합니다. */
    @Transactional(readOnly = true)
    public IdeaResponse getIdea(Long ideaId) {
        Idea idea = findActiveIdea(ideaId);
        List<MilestoneResponse> milestones = milestoneRepository.findByIdeaIdOrderByStep(ideaId)
                .stream()
                .map(MilestoneResponse::from)
                .toList();
        return IdeaResponse.of(idea, milestones);
    }

    /** 외부 API 요청용 아이디어 상세 조회 (권한 검사 있음) */
    /** 아이디어 상태와 요청자 권한에 따라 상세 정보를 반환합니다.
     * OPEN 이전은 작성자/전문가/관리자만, OPEN 이후는 모든 사용자가 조회 가능합니다. */
    @Transactional(readOnly = true)
    public IdeaResponse getIdea(Long ideaId, Long userId, Role role) {
        Idea idea = findActiveIdea(ideaId);

        boolean isOwner = userId != null && idea.getUserId().equals(userId);
        boolean isAdmin = role == Role.ADMIN;
        boolean isMatchedExpert = role == Role.EXPERT && userId != null
                && expertMatchRepository.existsByIdeaIdAndUserId(ideaId, userId);
        boolean isPublic = idea.getStatus() == IdeaStatus.OPEN
                || idea.getStatus() == IdeaStatus.IN_PROGRESS
                || idea.getStatus() == IdeaStatus.COMPLETED
                || idea.getStatus() == IdeaStatus.CANCELLATION_REQUESTED;

        if (!isPublic && !isOwner && !isAdmin && !isMatchedExpert) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        List<MilestoneResponse> milestones = milestoneRepository.findByIdeaIdOrderByStep(ideaId)
                .stream()
                .map(MilestoneResponse::from)
                .toList();
        return IdeaResponse.of(idea, milestones);
    }

    /** 알림 발송 등 내부 도메인에서 아이디어 제목과 작성자 ID만 필요할 때 사용합니다. */
    @Transactional(readOnly = true)
    public IdeaSummaryResponse getIdeaSummary(Long ideaId) {
        Idea idea = findActiveIdea(ideaId);
        return IdeaSummaryResponse.of(idea);
    }

    /** 작성자 본인이고 심사 대기 상태인 경우에만 아이디어 정보를 수정합니다. */
    @Transactional
    public IdeaResponse updateIdea(Long ideaId, Long userId, UpdateIdeaRequest request) {
        Idea idea = findActiveIdea(ideaId);
        idea.validateOwner(userId);
        validateDepositAmount(request.depositAmount(), request.goalAmount());
        // 수정 흐름에서도 엔티티 변경 전에 상태를 명시 검증해 승인 이후 수정을 차단합니다.
        idea.validateEditable();
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
                request.depositAmount(),
                request.fundingStartAt(),
                request.fundingEndAt(),
                request.rewardType(),
                request.imageUrl()
        );
        idea.updateImageUrls(ImageUrlConverter.join(request.imageUrls()));
        replaceMilestones(ideaId, request.milestones());
        resubmitRejectedIdea(idea);
        return IdeaResponse.of(idea);
    }

    /** 제안자가 본문 이미지를 아이디어 등록 전에 사전 업로드하고 URL 목록을 반환합니다. */
    public List<String> uploadContentImages(List<MultipartFile> images) {
        validateImageFiles(images);
        return images.stream()
                .map(image -> storageClient.upload(image, "ideas/content"))
                .toList();
    }

    /** 작성자 본인이고 심사 대기 상태인 경우에만 대표 이미지를 업로드하고 URL을 저장합니다. */
    @Transactional
    public IdeaResponse uploadIdeaImage(Long ideaId, Long userId, MultipartFile image) {
        validateImageFile(image);
        Idea idea = findActiveIdea(ideaId);
        idea.validateOwner(userId);
        idea.validateEditable();

        String imageUrl = storageClient.upload(image, "ideas/thumbnail");

        try {
            // 업로드와 DB 저장을 단일 서비스 흐름으로 묶고, 저장 실패 시 업로드 파일을 보상 삭제합니다.
            idea.updateImageUrl(imageUrl);
            ideaRepository.flush();
            return IdeaResponse.of(idea);
        } catch (RuntimeException e) {
            storageClient.delete(imageUrl);
            throw e;
        }
    }

    /** 제안자가 관리자 최종 승인 전에 정산 및 환불에 사용할 계좌 정보를 등록하거나 수정합니다. */
    @Transactional
    public IdeaSettlementAccountResponse upsertSettlementAccount(
            Long ideaId,
            Long userId,
            IdeaSettlementAccountRequest request
    ) {
        Idea idea = findActiveIdea(ideaId);
        idea.validateOwner(userId);
        validateSettlementAccountEditable(idea);

        IdeaSettlementAccount account = ideaSettlementAccountRepository.findByIdeaId(ideaId)
                .map(existingAccount -> {
                    existingAccount.update(
                            request.bankName(),
                            request.accountNumber(),
                            request.accountHolderName()
                    );
                    return existingAccount;
                })
                .orElseGet(() -> ideaSettlementAccountRepository.save(
                        IdeaSettlementAccount.create(
                                ideaId,
                                request.bankName(),
                                request.accountNumber(),
                                request.accountHolderName()
                        )
                ));

        return IdeaSettlementAccountResponse.of(account);
    }

    /** 제안자 본인이 등록한 정산 및 환불 계좌 정보를 조회합니다. */
    @Transactional(readOnly = true)
    public IdeaSettlementAccountResponse getSettlementAccount(Long ideaId, Long userId) {
        Idea idea = findActiveIdea(ideaId);
        idea.validateOwner(userId);

        IdeaSettlementAccount account = ideaSettlementAccountRepository.findByIdeaId(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.SETTLEMENT_ACCOUNT_NOT_REGISTERED));

        return IdeaSettlementAccountResponse.of(account);
    }

    /** 작성자 본인이고 심사 대기 상태인 경우에만 아이디어를 소프트 삭제합니다. */
    @Transactional
    public void deleteIdea(Long ideaId, Long userId) {
        Idea idea = findActiveIdea(ideaId);
        idea.validateOwner(userId);
        idea.validateDeletable(); // 명시적 상태 검증 추가
        milestoneRepository.deleteByIdeaIdBulk(ideaId);
        ideaBookmarkRepository.deleteByIdeaIdBulk(ideaId);
        ideaSettlementAccountRepository.deleteByIdeaIdBulk(ideaId);
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

    /** 업로드할 이미지 파일 목록이 비어 있지 않은지 검증합니다. */
    private void validateImageFiles(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (images.size() > MAX_CONTENT_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.IMAGE_COUNT_EXCEEDED);
        }
        images.forEach(this::validateImageFile);
    }

    /** 업로드할 이미지 파일이 비어 있지 않은지 검증합니다. */
    private void validateImageFile(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new CustomException(ErrorCode.IMAGE_SIZE_EXCEEDED);
        }
        if (!ALLOWED_MIME_TYPES.contains(image.getContentType())) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_TYPE);
        }
    }

    /** 관리자 최종 승인 전 또는 반려 시 제안자 계좌 등록/수정을 허용합니다. */
    private void validateSettlementAccountEditable(Idea idea) {
        idea.validateEditable();
    }

    /** 수정 요청에 마일스톤이 포함되면 기존 마일스톤을 삭제하고 새 목록으로 교체합니다. */
    private void replaceMilestones(Long ideaId, List<CreateMilestoneRequest> milestones) {
        if (milestones == null) {
            return;
        }
        validateMilestones(milestones);
        milestoneRepository.deleteByIdeaIdBulk(ideaId);
        milestoneRepository.saveAll(
                milestones.stream()
                        .map(m -> Milestone.builder()
                                .ideaId(ideaId)
                                .step(m.step())
                                .goal(m.goal())
                                .expectedResult(m.expectedResult())
                                .expectedDate(m.expectedDate())
                                .build())
                        .toList()
        );
    }

    /** 관리자 반려 상태의 아이디어를 수정하면 AI 재심사 대기 상태로 되돌립니다. */
    private void resubmitRejectedIdea(Idea idea) {
        if (idea.getStatus() == IdeaStatus.REJECTED) {
            idea.changeStatus(IdeaStatus.AI_PENDING);
            verificationService.requestVerification(
                    new VerificationRequest(
                            idea.getId(),
                            idea.getTitle(),
                            buildVerificationDescription(idea),
                            milestoneRepository.findByIdeaIdOrderByStep(idea.getId()).stream()
                                    .map(m -> new VerificationRequest.MilestoneInfo(
                                            m.getGoal(),
                                            m.getExpectedResult(),
                                            m.getExpectedDate(),
                                            null
                                    ))
                                    .toList()
                    ),
                    idea.getUserId()
            );
        }
    }

    /** AI 검증에 전달할 아이디어 상세 설명을 Idea 엔티티에서 생성합니다. */
    private String buildVerificationDescription(Idea idea) {
        return Stream.of(
                        idea.getOneLineIntro(),
                        idea.getProblemDefinition(),
                        idea.getSolution(),
                        idea.getGoal(),
                        idea.getTargetCustomer(),
                        idea.getCompetitor(),
                        idea.getTeamIntro()
                )
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }

    /** 임시저장을 조회하고 작성자 본인 접근인지 검증합니다. */
    private IdeaDraft findDraft(Long draftId, Long userId) {
        IdeaDraft draft = ideaDraftRepository.findById(draftId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_DRAFT_NOT_FOUND));
        draft.validateOwner(userId);
        if (draft.getUpdatedAt().isBefore(draftRetentionStartAt())) {
            throw new CustomException(ErrorCode.IDEA_DRAFT_NOT_FOUND);
        }
        return draft;
    }

    /** 관리자 일시 중단 상태인 경우 예외를 발생시킵니다. */
    public void validateNotSuspended(Long ideaId) {
        Idea idea = findActiveIdea(ideaId);
        if (idea.getStatus() == IdeaStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.IDEA_SUSPENDED);
        }
    }

    /** 소프트 삭제되지 않은 아이디어를 조회하고 없으면 공통 예외를 발생시킵니다. */
    private Idea findActiveIdea(Long ideaId) {
        return ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
    }

    /** 아이디어 생성 요청에서 마일스톤을 추출해 검증합니다. */
    private void validateMilestones(CreateIdeaRequest request) {
        validateMilestones(request.milestones());
    }

    /** 마일스톤 개수와 단계 값이 정확히 1, 2, 3인지 검증합니다. */
    private void validateMilestones(List<CreateMilestoneRequest> milestones) {
        if (milestones == null || milestones.size() != REQUIRED_MILESTONE_COUNT) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_COUNT);
        }

        Set<Integer> steps = new HashSet<>();
        for (var milestone : milestones) {
            if (milestone == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            steps.add(milestone.step());
        }

        if (!steps.containsAll(List.of(1, 2, 3)) || steps.size() != REQUIRED_MILESTONE_COUNT) {
            throw new CustomException(ErrorCode.INVALID_MILESTONE_STEP);
        }
    }

    /** 3단계 마일스톤 완료 후 아이디어를 완료 상태로 전이합니다. */
    @Transactional
    public void completeIdea(Long ideaId) {
        Idea idea = findActiveIdea(ideaId);
        idea.changeStatus(IdeaStatus.COMPLETED);
    }

    /** 펀딩 마감됐고 목표 금액 미달성인 아이디어 ID 목록을 반환합니다. */
    @Transactional(readOnly = true)
    public List<Long> getFailedFundingIdeaIds() {
        return ideaRepository.findFailedFundingIdeaIds(LocalDateTime.now());
    }

    /** 펀딩 목표 미달성 시 아이디어를 취소 상태로 전이합니다. */
    @Transactional
    public void cancelIdea(Long ideaId) {
        Idea idea = findActiveIdea(ideaId);
        idea.changeStatus(IdeaStatus.CANCELLED);
    }
}