package com.team04.domain.funding.service;

import com.team04.domain.funding.dto.request.OpenFundingRequest;
import com.team04.domain.funding.dto.request.PayDepositRequest;
import com.team04.domain.funding.dto.request.SponsorRequest;
import com.team04.domain.funding.dto.response.CreateFundingResponse;
import com.team04.domain.funding.dto.response.DepositResponse;
import com.team04.domain.funding.dto.response.FundingAchievementResponse;
import com.team04.domain.funding.dto.response.FundingDetailResponse;
import com.team04.domain.funding.dto.response.FundingSummaryResponse;
import com.team04.domain.funding.entity.Deposit;
import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.entity.FundingTypes.DepositStatus;
import com.team04.domain.funding.entity.FundingTypes.FundingStatus;
import com.team04.domain.funding.event.FundingPaidEvent;
import com.team04.domain.funding.repository.DepositRepository;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.entity.MilestoneStatus;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.domain.payment.dto.request.CreatePaymentRequest;
import com.team04.domain.payment.dto.response.PaymentResponse;
import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes.PaymentStatus;
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.domain.payment.service.IdeaVbankPoolService;
import com.team04.domain.payment.service.PaymentService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 펀딩 도메인 비즈니스 로직을 담당하는 서비스입니다.
 * 보증금 납부, 펀딩 오픈·조회, 후원 신청·취소, 달성률 SSE 전송을 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundingService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final FundingRepository fundingRepository;
    private final IdeaRepository ideaRepository;
    private final DepositRepository depositRepository;
    private final MilestoneRepository milestoneRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final IdeaVbankPoolService ideaVbankPoolService;

    /** fundingId(ideaId)별 SSE 구독자 목록 */
    private final Map<Long, Set<SseEmitter>> sseEmitters = new ConcurrentHashMap<>();

    // ── 보증금 ──────────────────────────────────────────────────────────────

    // 프로젝트 보증금 조회
    @Transactional(readOnly = true)
    public DepositResponse getDeposit(Long ideaId) {
        Deposit deposit = depositRepository.findByIdeaId(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESCROW_NOT_FOUND));
        return DepositResponse.from(deposit);
    }

    // 창작자 보증금 납부 — PG 결제 세션 생성 (결제 완료 후 Deposit HELD)
    @Transactional
    public DepositResponse payDeposit(Long ideaId, Long userId, PayDepositRequest request) {
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
        idea.validateOwner(userId);

        if (depositRepository.existsByIdeaIdAndStatus(ideaId, DepositStatus.HELD)) {
            throw new CustomException(ErrorCode.PROJECT_FEE_ALREADY_PAID);
        }

        if (request.amount() == null || request.amount() < 1) {
            throw new CustomException(ErrorCode.INVALID_FUNDING_AMOUNT);
        }

        // 보증금은 목표 펀딩액의 30% 이하여야 함
        if (idea.getGoalAmount() != null && request.amount() > Math.round(idea.getGoalAmount() * 0.3)) {
            throw new CustomException(ErrorCode.DEPOSIT_EXCEEDS_LIMIT);
        }

        if (idea.getDepositAmount() != null
                && idea.getDepositAmount() > 0
                && !idea.getDepositAmount().equals(request.amount())) {
            throw new CustomException(ErrorCode.DEPOSIT_AMOUNT_MISMATCH);
        }

        PaymentResponse payment = paymentService.createDepositPayment(
                ideaId,
                userId,
                request.amount(),
                request.paymentMethod()
        );
        return DepositResponse.pendingPayment(ideaId, userId, request.amount(), payment);
    }

    // 조건 충족 시 창작자에게 보증금 환급(REFUNDED)
    @Transactional
    public DepositResponse releaseDeposit(Long ideaId, Long userId) {
        Deposit deposit = getHeldDeposit(ideaId, userId);
        deposit.release();
        return DepositResponse.from(deposit);
    }

    // 정당한 사유 중단 시 보증금 환급 — 소유자 검증 없이 시스템이 직접 처리
    @Transactional
    public DepositResponse releaseDeposit(Long ideaId) {
        Deposit deposit = depositRepository.findByIdeaId(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESCROW_NOT_FOUND));
        deposit.release();
        return DepositResponse.from(deposit);
    }

    // 프로젝트 실패·위반 시 보증금 몰수(FORFEITED)
    @Transactional
    public DepositResponse forfeitDeposit(Long ideaId) {
        Deposit deposit = depositRepository.findByIdeaId(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESCROW_NOT_FOUND));
        deposit.forfeit();
        return DepositResponse.from(deposit);
    }

    // ── 펀딩 캠페인 ────────────────────────────────────────────────────────

    // 보증금 납부 확인 후 펀딩 오픈 — 펀딩 시작일 도래 시 IN_PROGRESS 전환
    @Transactional
    public FundingDetailResponse openFunding(OpenFundingRequest request, Long proposerId) {
        Idea idea = ideaRepository.findByIdForUpdate(request.ideaId())
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
        idea.validateOwner(proposerId);

        if (!depositRepository.existsByIdeaIdAndStatus(request.ideaId(), DepositStatus.HELD)) {
            throw new CustomException(ErrorCode.ESCROW_NOT_FOUND);
        }

        if (idea.getStatus() != IdeaStatus.OPEN && idea.getStatus() != IdeaStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.IDEA_NOT_OPEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (idea.getStatus() == IdeaStatus.OPEN && !now.isBefore(idea.getFundingStartAt())) {
            idea.changeStatus(IdeaStatus.IN_PROGRESS);
        }

        if (ideaVbankPoolService.usePoolForCreateTimeVbank()) {
            ideaVbankPoolService.ensurePoolForIdea(request.ideaId());
        }

        return FundingDetailResponse.from(idea);
    }

    // OPEN/IN_PROGRESS 상태 펀딩 목록 조회 (페이징)
    @Transactional(readOnly = true)
    public Page<FundingSummaryResponse> getFundings(Pageable pageable) {
        return ideaRepository.findByStatusInAndDeletedAtIsNull(
                List.of(IdeaStatus.OPEN, IdeaStatus.IN_PROGRESS),
                pageable
        ).map(FundingSummaryResponse::from);
    }

    // 펀딩 상세 조회 — 목표금액·현재금액·달성률 포함 (fundingId = ideaId)
    @Transactional(readOnly = true)
    public FundingDetailResponse getFundingDetail(Long fundingId) {
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(fundingId)
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));
        return FundingDetailResponse.from(idea);
    }

    // ── 후원 ──────────────────────────────────────────────────────────────

    // 후원 신청 — Funding(PENDING_PAYMENT) 생성 후 결제 세션 반환
    public CreateFundingResponse applySponsorship(Long fundingId, Long sponsorId, SponsorRequest request) {
        Idea idea = validateFundableIdea(fundingId, sponsorId, request.amount());
        int milestoneStep = resolveMilestoneStep(fundingId);

        Funding funding = fundingRepository.save(Funding.createPending(
                fundingId,
                sponsorId,
                milestoneStep,
                request.amount(),
                idea.getRewardType()
        ));

        PaymentResponse payment = paymentService.createPayment(new CreatePaymentRequest(
                funding.getId(),
                request.amount(),
                request.paymentMethod()
        ), sponsorId);

        return CreateFundingResponse.from(funding, payment);
    }

    // 내 후원 취소 — PAID면 5단계 환불, PENDING_PAYMENT면 결제 실패 처리
    @Transactional
    public void cancelMySponsorship(Long fundingId, Long sponsorId) {
        Funding funding = fundingRepository
                .findFirstByIdeaIdAndSponsorIdAndStatusInOrderByCreatedAtDesc(
                        fundingId,
                        sponsorId,
                        List.of(FundingStatus.PAID, FundingStatus.PENDING_PAYMENT)
                )
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        if (funding.getStatus() == FundingStatus.PAID) {
            Payment payment = paymentRepository
                    .findFirstByFundingIdAndStatusOrderByCreatedAtDesc(funding.getId(), PaymentStatus.SUCCESS)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
            paymentService.refundPayment(payment.getId(), sponsorId);
            return;
        }

        Payment payment = paymentRepository
                .findFirstByFundingIdAndStatusOrderByCreatedAtDesc(funding.getId(), PaymentStatus.PENDING)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.fail();
        funding.markAsCancelled();
    }

    // ── SSE (달성률 실시간 스트리밍) ───────────────────────────────────────

    // 펀딩 달성률 SSE 구독 연결
    public SseEmitter subscribeAchievement(Long fundingId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        sseEmitters.computeIfAbsent(fundingId, id -> ConcurrentHashMap.newKeySet()).add(emitter);

        emitter.onCompletion(() -> removeSseEmitter(fundingId, emitter));
        emitter.onTimeout(() -> removeSseEmitter(fundingId, emitter));
        emitter.onError(error -> removeSseEmitter(fundingId, emitter));

        return emitter;
    }

    // 결제 완료 이벤트 수신 시 구독 중인 클라이언트에 달성률 push
    public void notifyAchievementUpdate(FundingPaidEvent event) {
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(event.ideaId()).orElse(null);
        if (idea == null) {
            return;
        }

        FundingDetailResponse detail = FundingDetailResponse.from(idea);
        sendAchievementRate(event.ideaId(), FundingAchievementResponse.from(detail));
    }

    private void sendAchievementRate(Long fundingId, FundingAchievementResponse event) {
        Set<SseEmitter> subscribers = sseEmitters.get(fundingId);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : subscribers) {
            try {
                emitter.send(SseEmitter.event().name("achievement").data(event));
            } catch (IOException e) {
                log.debug("SSE 전송 실패 fundingId={}", fundingId, e);
                removeSseEmitter(fundingId, emitter);
            }
        }
    }

    private void removeSseEmitter(Long fundingId, SseEmitter emitter) {
        Set<SseEmitter> subscribers = sseEmitters.get(fundingId);
        if (subscribers == null) {
            return;
        }
        subscribers.remove(emitter);
        if (subscribers.isEmpty()) {
            sseEmitters.remove(fundingId);
        }
    }

    // ── 내부 검증 ──────────────────────────────────────────────────────────

    private Deposit getHeldDeposit(Long ideaId, Long userId) {
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
        idea.validateOwner(userId);

        return depositRepository.findByIdeaId(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESCROW_NOT_FOUND));
    }

    private int resolveMilestoneStep(Long ideaId) {
        return milestoneRepository.findByIdeaIdAndStatus(ideaId, MilestoneStatus.IN_PROGRESS)
                .map(Milestone::getStep)
                .orElseGet(() -> milestoneRepository.findByIdeaIdOrderByStep(ideaId).stream()
                        .filter(milestone -> milestone.getStatus() == MilestoneStatus.PENDING)
                        .mapToInt(Milestone::getStep)
                        .max()
                        .orElse(0));
    }

    private Idea validateFundableIdea(Long ideaId, Long sponsorId, Long amount) {
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));

        if (idea.getStatus() != IdeaStatus.OPEN && idea.getStatus() != IdeaStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.IDEA_NOT_OPEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(idea.getFundingStartAt()) || now.isAfter(idea.getFundingEndAt())) {
            throw new CustomException(ErrorCode.FUNDING_ALREADY_CLOSED);
        }

        if (idea.getUserId().equals(sponsorId)) {
            throw new CustomException(ErrorCode.IDEA_SELF_FUNDING_NOT_ALLOWED);
        }

        if (amount == null || amount < 1) {
            throw new CustomException(ErrorCode.INVALID_FUNDING_AMOUNT);
        }

        return idea;
    }
    /**
     * 펀딩 목표 달성 여부 확인
     * currentAmount >= goalAmount 이면 목표 달성
     * FundingAchievementListener에서 SSE push에 사용 (목표 달성·마일스톤 시작은 본 리스너에서 처리)
     */
    public boolean isFundingGoalAchieved(Long ideaId) {
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                .orElse(null);
        if (idea == null) {
            return false;
        }
        return idea.getCurrentAmount() >= idea.getGoalAmount();
    }
}