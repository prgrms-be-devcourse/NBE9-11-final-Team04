package com.team04.domain.dispute.service;

import com.team04.domain.dispute.dto.request.CreateAppealRequest;
import com.team04.domain.dispute.dto.request.CreateDisputeRequest;
import com.team04.domain.dispute.dto.response.DisputeResponse;
import com.team04.domain.dispute.entity.Dispute;
import com.team04.domain.dispute.entity.DisputeAppeal;
import com.team04.domain.dispute.repository.DisputeAppealRepository;
import com.team04.domain.dispute.repository.DisputeRepository;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.global.event.ReportNotificationEvent;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeAppealRepository disputeAppealRepository;
    private final DisputeRepository disputeRepository;
    private final IdeaRepository ideaRepository;
    private final UserRepository userRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DisputeResponse createDispute(Long reporterId, CreateDisputeRequest request) {


        Idea idea = ideaRepository.findById(request.ideaId())
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));

        if (reporterId.equals(idea.getUserId())) {
            throw new CustomException(ErrorCode.DISPUTE_CANNOT_REPORT_YOURSELF);
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        User proposer = userRepository.findById(idea.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Dispute dispute = new Dispute(
                idea,
                reporter,
                proposer,
                request.reason(),
                request.evidenceUrl()
        );

        disputeRepository.save(dispute);

        eventPublisher.publishEvent(new ReportNotificationEvent(
                dispute.getId(),
                "DISPUTE",
                reporterId,
                request.reason()
        ));

        return DisputeResponse.of(dispute);
    }

    @Transactional(readOnly = true)
    public DisputeResponse getDispute(Long userId, Long disputeId) {
        Dispute dispute = disputeRepository.findByIdWithDetails(disputeId)
                .orElseThrow(() -> new CustomException(ErrorCode.DISPUTE_NOT_FOUND));

        boolean isAdmin = userRepository.findById(userId)
                .map(u -> u.getRole() == Role.ADMIN).orElse(false);

        if (!isAdmin && !userId.equals(dispute.getReporter().getId()) &&
                !userId.equals(dispute.getProposer().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return DisputeResponse.of(dispute);
    }

    @Transactional
    public void createAppeal(Long disputeId, Long userId, CreateAppealRequest request){
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new CustomException(ErrorCode.DISPUTE_NOT_FOUND));

        // 본인 확인 먼저
        if (!userId.equals(dispute.getProposer().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 있으면 수정, 없으면 생성
        disputeAppealRepository.findByDisputeId(disputeId)
                .ifPresentOrElse(
                        appeal -> appeal.update(request.content(), request.fileUrl()),
                        () -> disputeAppealRepository.save(
                                new DisputeAppeal(dispute, request.content(), request.fileUrl()))
                );
    }
}
