package com.team04.domain.idea.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 아이디어 등록, 조회, 수정, 삭제, 신고 비즈니스 로직을 처리하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class IdeaService {

    private static final int REQUIRED_MILESTONE_COUNT = 3;

    private final IdeaRepository ideaRepository;
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
        eventPublisher.publishEvent(
                new IdeaPlagiarismReportedEvent(idea.getId(), idea.getUserId(), reporterUserId, request.reason())
        );
        eventPublisher.publishEvent(
                new IdeaReportNotificationEvent(idea.getId(), reporterUserId, request.reason())
        );
        return new ReportIdeaResponse(idea.getId(), reporterUserId, "아이디어 도용 신고가 접수되었습니다.");
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