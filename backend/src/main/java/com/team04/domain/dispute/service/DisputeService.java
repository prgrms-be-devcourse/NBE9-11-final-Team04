package com.team04.domain.dispute.service;

import com.team04.domain.dispute.dto.request.AdminDisputeStatusRequest;
import com.team04.domain.dispute.dto.request.CreateAppealRequest;
import com.team04.domain.dispute.dto.request.CreateDisputeRequest;
import com.team04.domain.dispute.dto.request.ForceRefundRequest;
import com.team04.domain.settlement.dto.response.RefundResponse;
import com.team04.domain.settlement.service.RefundService;
import com.team04.domain.settlement.service.SettlementService;
import com.team04.domain.dispute.dto.response.AdminDisputeResponse;
import com.team04.domain.dispute.dto.response.DisputeResponse;
import com.team04.domain.dispute.dto.response.DisputeStatsResponse;
import com.team04.domain.dispute.entity.*;
import com.team04.domain.dispute.repository.DisputeAppealRepository;
import com.team04.domain.dispute.repository.DisputeRepository;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.domain.notification.entity.NotificationPriority;
import com.team04.global.event.NotificationEvent;
import com.team04.global.event.ReportNotificationEvent;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.storage.StorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final DisputeAppealRepository disputeAppealRepository;
    private final UserRepository userRepository;
    private final DisputeParticipantValidator participantValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final StorageClient storageClient;
    private final RefundService refundService;

    @Lazy
    @Autowired
    private SettlementService settlementService;

    private static final List<DisputeStatus> ACTIVE_STATUSES = List.of(DisputeStatus.RECEIVED, DisputeStatus.PENDING);
    private static final String APPEAL_STORAGE_DIR = "dispute/appeal";

    @Transactional
    public DisputeResponse createDispute(Long reporterId, CreateDisputeRequest request) {

        if (reporterId.equals(request.reportedUserId())) {
            throw new CustomException(ErrorCode.DISPUTE_CANNOT_REPORT_YOURSELF);
        }

        participantValidator.validate(request.targetType(), request.targetId(), request.reportedUserId());


        if (disputeRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
                reporterId, request.targetType(), request.targetId(), ACTIVE_STATUSES)) {
            throw new CustomException(ErrorCode.DISPUTE_ALREADY_PENDING);
        }

        User reporter = userRepository.getReferenceById(reporterId);
        User reported = userRepository.getReferenceById(request.reportedUserId());

        Dispute dispute = new Dispute(
                reporter, reported,
                request.targetType(), request.targetId(),
                request.category(), request.title(),
                request.reason(), request.evidenceUrl()
        );
        Dispute saved = disputeRepository.save(dispute);

        eventPublisher.publishEvent(new ReportNotificationEvent(
                Role.ADMIN,
                NotificationType.REPORT_RECEIVED,
                "[신고] " + request.title(),
                request.category().name() + " | " + request.reason(),
                saved.getId()
        ));

        eventPublisher.publishEvent(new NotificationEvent(
                request.reportedUserId(),
                NotificationType.DISPUTE_REPORTED,
                "[신고 접수] " + request.title(),
                "회원님을 대상으로 신고가 접수되었습니다. 7일 이내 소명 자료를 제출해 주세요.",
                saved.getId()
        ));

        return DisputeResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public DisputeResponse getDispute(Long userId, Long disputeId, Role role) {
        Dispute dispute = disputeRepository.findByIdWithDetails(disputeId)
                .orElseThrow(() -> new CustomException(ErrorCode.DISPUTE_NOT_FOUND));
        if (role != Role.ADMIN
                && !userId.equals(dispute.getReporter().getId())
                && !userId.equals(dispute.getReported().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return DisputeResponse.of(dispute);
    }

    @Transactional
    public void createAppeal(Long disputeId, Long userId, CreateAppealRequest request, MultipartFile file) {
        Dispute dispute = disputeRepository.findByIdWithDetails(disputeId)
                .orElseThrow(() -> new CustomException(ErrorCode.DISPUTE_NOT_FOUND));
        if (!userId.equals(dispute.getReported().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (!dispute.isAppealable()) {
            throw new CustomException(ErrorCode.DISPUTE_APPEAL_NOT_ALLOWED);
        }

        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            fileUrl = storageClient.upload(file, APPEAL_STORAGE_DIR);
        }

        final String resolvedFileUrl = fileUrl;
        disputeAppealRepository.findByDisputeId(disputeId)
                .ifPresentOrElse(
                        appeal -> appeal.update(request.content(), resolvedFileUrl),
                        () -> disputeAppealRepository.save(new DisputeAppeal(dispute, request.content(), resolvedFileUrl))
                );

        dispute.updateStatus(DisputeStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public Page<AdminDisputeResponse> getDisputeList(
            DisputeStatus status, DisputeCategory category, TargetType targetType, Pageable pageable) {
        return disputeRepository.findAllByFilters(status, category, targetType, pageable)
                .map(AdminDisputeResponse::of);
    }

    @Transactional(readOnly = true)
    public DisputeStatsResponse getDisputeStats() {
        Map<DisputeStatus, Long> countMap = disputeRepository.countGroupByStatus().stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (DisputeStatus) row[0],
                        row -> (Long) row[1]
                ));
        long received = countMap.getOrDefault(DisputeStatus.RECEIVED, 0L);
        long pending  = countMap.getOrDefault(DisputeStatus.PENDING, 0L);
        long resolved = countMap.getOrDefault(DisputeStatus.RESOLVED, 0L);
        long rejected = countMap.getOrDefault(DisputeStatus.REJECTED, 0L);
        return new DisputeStatsResponse(received + pending + resolved + rejected, received, pending, resolved, rejected);
    }

    @Transactional
    public RefundResponse forceRefund(Long disputeId, ForceRefundRequest request) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new CustomException(ErrorCode.DISPUTE_NOT_FOUND));
        if (dispute.getStatus() != DisputeStatus.RESOLVED) {
            throw new CustomException(ErrorCode.DISPUTE_INVALID_STATUS_TRANSITION);
        }
        if (dispute.getTargetType() != TargetType.IDEA) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        Long ideaId = dispute.getTargetId();
        return refundService.createDisputeRefund(request.paymentId(), ideaId);
    }

    @Transactional
    public void forceRefundAll(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new CustomException(ErrorCode.DISPUTE_NOT_FOUND));
        if (dispute.getStatus() != DisputeStatus.RESOLVED) {
            throw new CustomException(ErrorCode.DISPUTE_INVALID_STATUS_TRANSITION);
        }
        if (dispute.getTargetType() != TargetType.IDEA) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        Long ideaId = dispute.getTargetId();
        settlementService.createCancelRefundSettlement(ideaId);
        settlementService.createDepositForfeitSettlement(ideaId);
        refundService.createCancelRefunds(ideaId, false);
    }

    @Transactional
    public DisputeResponse updateDisputeStatus(Long disputeId, AdminDisputeStatusRequest request) {
        Dispute dispute = disputeRepository.findByIdWithDetails(disputeId)
                .orElseThrow(() -> new CustomException(ErrorCode.DISPUTE_NOT_FOUND));
        dispute.updateStatus(request.status());
        syncAppealStatus(disputeId, request.status());
        publishDisputeStatusNotifications(dispute, request.status());
        return DisputeResponse.of(dispute);
    }

    private void syncAppealStatus(Long disputeId, DisputeStatus newStatus) {
        if (newStatus == DisputeStatus.RECEIVED) {
            disputeAppealRepository.findByDisputeId(disputeId)
                    .ifPresent(DisputeAppeal::reject);
        } else if (newStatus == DisputeStatus.REJECTED) {
            disputeAppealRepository.findByDisputeId(disputeId)
                    .ifPresent(DisputeAppeal::approve);
        }
    }

    private void publishDisputeStatusNotifications(Dispute dispute, DisputeStatus newStatus) {
        Long reporterId = dispute.getReporter().getId();
        Long reportedId = dispute.getReported().getId();
        String title = dispute.getTitle();

        if (newStatus == DisputeStatus.PENDING) {
            eventPublisher.publishEvent(new NotificationEvent(
                    reportedId, NotificationType.DISPUTE_UNDER_REVIEW,
                    "[신고 검토 시작] " + title, "관리자가 신고 내용을 검토하고 있습니다", dispute.getId()
            ));
            return;
        }

        if (newStatus == DisputeStatus.RECEIVED) {
            eventPublisher.publishEvent(new NotificationEvent(
                    reportedId, NotificationType.DISPUTE_UNDER_REVIEW,
                    "[소명 재제출 요청] " + title, "관리자가 소명 자료 보완을 요청했습니다", dispute.getId()
            ));
            return;
        }

        NotificationType type = newStatus == DisputeStatus.RESOLVED
                ? NotificationType.DISPUTE_RESOLVED
                : NotificationType.DISPUTE_REJECTED;
        String message = newStatus == DisputeStatus.RESOLVED ? "신고가 처리되었습니다" : "신고가 기각되었습니다";

        eventPublisher.publishEvent(new NotificationEvent(
                reporterId, type, "[분쟁 처리 결과] " + title, message, dispute.getId(), NotificationPriority.CRITICAL
        ));
        eventPublisher.publishEvent(new NotificationEvent(
                reportedId, type, "[분쟁 처리 결과] " + title, message, dispute.getId(), NotificationPriority.CRITICAL
        ));
    }
}
