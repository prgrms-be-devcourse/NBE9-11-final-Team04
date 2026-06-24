package com.team04.domain.dispute.service;

import com.team04.domain.dispute.dto.request.CreateAppealRequest;
import com.team04.domain.dispute.dto.request.CreateDisputeRequest;
import com.team04.domain.dispute.dto.response.DisputeResponse;
import com.team04.domain.dispute.entity.*;
import com.team04.domain.dispute.repository.DisputeAppealRepository;
import com.team04.domain.dispute.repository.DisputeRepository;
import com.team04.domain.idea.service.IdeaAdminService;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.settlement.service.RefundService;
import com.team04.domain.settlement.service.SettlementService;
import com.team04.global.storage.StorageClient;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.domain.dispute.dto.request.AdminDisputeStatusRequest;
import com.team04.global.event.NotificationEvent;
import com.team04.global.event.ReportNotificationEvent;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock private DisputeRepository disputeRepository;
    @Mock private DisputeAppealRepository disputeAppealRepository;
    @Mock private UserRepository userRepository;
    @Mock private DisputeParticipantValidator participantValidator;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private StorageClient storageClient;
    @Mock private RefundService refundService;
    @Mock private SettlementService settlementService;
    @Mock private IdeaAdminService ideaAdminService;

    @InjectMocks
    private DisputeService disputeService;

    private User reporter;
    private User reported;
    private Dispute dispute;

    @BeforeEach
    void setUp() {
        reporter = User.create("reporter@test.com", "pw", "신고자", "reporter", 30, Role.USER);
        ReflectionTestUtils.setField(reporter, "id", 1L);

        reported = User.create("reported@test.com", "pw", "피신고자", "reported", 30, Role.USER);
        ReflectionTestUtils.setField(reported, "id", 2L);

        dispute = new Dispute(reporter, reported, TargetType.IDEA, 10L,
                DisputeCategory.IDEA_THEFT, "아이디어 도용 신고", "명백한 도용입니다", null);
        ReflectionTestUtils.setField(dispute, "id", 1L);
        ReflectionTestUtils.setField(dispute, "createdAt", java.time.LocalDateTime.now());
    }

    // ─────────────────────────────────────────────
    // createDispute
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("신고 생성 성공")
    void createDispute_성공() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                TargetType.IDEA, 10L, 2L, DisputeCategory.IDEA_THEFT, "아이디어 도용 신고", "명백한 도용입니다", null);

        given(disputeRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(any(), any(), any(), any()))
                .willReturn(false);
        given(userRepository.getReferenceById(1L)).willReturn(reporter);
        given(userRepository.getReferenceById(2L)).willReturn(reported);
        given(disputeRepository.save(any())).willReturn(dispute);

        DisputeResponse response = disputeService.createDispute(1L, request);

        assertThat(response.status()).isEqualTo(DisputeStatus.RECEIVED);
        assertThat(response.reporterId()).isEqualTo(1L);
        assertThat(response.reportedId()).isEqualTo(2L);

        ArgumentCaptor<ReportNotificationEvent> captor = ArgumentCaptor.forClass(ReportNotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().notificationType()).isEqualTo(NotificationType.REPORT_RECEIVED);
    }

    @Test
    @DisplayName("본인을 신고하면 예외 발생")
    void createDispute_본인신고_예외() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                TargetType.IDEA, 10L, 1L, DisputeCategory.IDEA_THEFT, "제목", "이유", null);

        assertThatThrownBy(() -> disputeService.createDispute(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DISPUTE_CANNOT_REPORT_YOURSELF);
    }

    @Test
    @DisplayName("진행 중인 동일 신고가 있으면 예외 발생")
    void createDispute_중복신고_예외() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                TargetType.IDEA, 10L, 2L, DisputeCategory.IDEA_THEFT, "제목", "이유", null);

        given(disputeRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(any(), any(), any(), any()))
                .willReturn(true);

        assertThatThrownBy(() -> disputeService.createDispute(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DISPUTE_ALREADY_PENDING);
    }

    // ─────────────────────────────────────────────
    // getDispute
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("관리자는 모든 분쟁 조회 가능")
    void getDispute_관리자_성공() {
        given(disputeRepository.findByIdWithDetails(1L)).willReturn(Optional.of(dispute));

        DisputeResponse response = disputeService.getDispute(99L, 1L, Role.ADMIN);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("신고자 본인은 분쟁 조회 가능")
    void getDispute_신고자_성공() {
        given(disputeRepository.findByIdWithDetails(1L)).willReturn(Optional.of(dispute));

        DisputeResponse response = disputeService.getDispute(1L, 1L, Role.USER);

        assertThat(response.reporterId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("당사자가 아닌 사용자는 분쟁 조회 불가")
    void getDispute_제3자_예외() {
        given(disputeRepository.findByIdWithDetails(1L)).willReturn(Optional.of(dispute));

        assertThatThrownBy(() -> disputeService.getDispute(99L, 1L, Role.USER))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    // ─────────────────────────────────────────────
    // createAppeal
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("RECEIVED 상태에서 소명 최초 제출 성공")
    void createAppeal_RECEIVED상태_성공() {
        CreateAppealRequest request = new CreateAppealRequest("소명 내용입니다");
        given(disputeRepository.findByIdWithDetails(1L)).willReturn(Optional.of(dispute));
        given(disputeAppealRepository.findByDisputeId(1L)).willReturn(Optional.empty());

        disputeService.createAppeal(1L, 2L, request, null);

        verify(disputeAppealRepository).save(any(DisputeAppeal.class));
    }

    @Test
    @DisplayName("RECEIVED 상태에서 소명 재제출 시 기존 내용 수정")
    void createAppeal_기존소명_수정() {
        CreateAppealRequest request = new CreateAppealRequest("수정된 소명");
        DisputeAppeal existingAppeal = new DisputeAppeal(dispute, "기존 소명", null);

        given(disputeRepository.findByIdWithDetails(1L)).willReturn(Optional.of(dispute));
        given(disputeAppealRepository.findByDisputeId(1L)).willReturn(Optional.of(existingAppeal));

        disputeService.createAppeal(1L, 2L, request, null);

        assertThat(existingAppeal.getContent()).isEqualTo("수정된 소명");
        verify(disputeAppealRepository, never()).save(any());
    }

    @Test
    @DisplayName("PENDING 상태에서 소명 제출 불가")
    void createAppeal_PENDING상태_예외() {
        dispute.updateStatus(DisputeStatus.PENDING);
        CreateAppealRequest request = new CreateAppealRequest("소명 내용");

        given(disputeRepository.findByIdWithDetails(1L)).willReturn(Optional.of(dispute));

        assertThatThrownBy(() -> disputeService.createAppeal(1L, 2L, request, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DISPUTE_APPEAL_NOT_ALLOWED);
    }

    @Test
    @DisplayName("피신고자가 아닌 사용자는 소명 제출 불가")
    void createAppeal_피신고자아님_예외() {
        CreateAppealRequest request = new CreateAppealRequest("소명 내용");
        given(disputeRepository.findByIdWithDetails(1L)).willReturn(Optional.of(dispute));

        assertThatThrownBy(() -> disputeService.createAppeal(1L, 99L, request, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    // ─────────────────────────────────────────────
    // updateDisputeStatus
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("RECEIVED → PENDING 전이 시 피신고자에게 알림 발행")
    void updateDisputeStatus_RECEIVED에서PENDING_피신고자알림() {
        given(disputeRepository.findByIdWithDetails(1L)).willReturn(Optional.of(dispute));

        disputeService.updateDisputeStatus(1L, new AdminDisputeStatusRequest(DisputeStatus.PENDING));

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NotificationEvent event = captor.getValue();
        assertThat(event.userId()).isEqualTo(2L);
        assertThat(event.notificationType()).isEqualTo(NotificationType.DISPUTE_UNDER_REVIEW);
    }

    @Test
    @DisplayName("PENDING → RESOLVED 전이 시 신고자·피신고자 모두 알림 발행")
    void updateDisputeStatus_PENDING에서RESOLVED_양측알림() {
        dispute.updateStatus(DisputeStatus.PENDING);
        given(disputeRepository.findByIdWithDetails(1L)).willReturn(Optional.of(dispute));

        disputeService.updateDisputeStatus(1L, new AdminDisputeStatusRequest(DisputeStatus.RESOLVED));

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());

        List<NotificationEvent> events = captor.getAllValues();
        assertThat(events).extracting(NotificationEvent::notificationType)
                .containsOnly(NotificationType.DISPUTE_RESOLVED);
        assertThat(events).extracting(NotificationEvent::userId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("유효하지 않은 상태 전이는 예외 발생")
    void updateDisputeStatus_유효하지않은전이_예외() {
        given(disputeRepository.findByIdWithDetails(1L)).willReturn(Optional.of(dispute));

        assertThatThrownBy(() -> disputeService.updateDisputeStatus(
                1L, new AdminDisputeStatusRequest(DisputeStatus.RESOLVED)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DISPUTE_INVALID_STATUS_TRANSITION);
    }
}
