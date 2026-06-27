package com.team04.domain.notification.service;

import com.team04.domain.notification.entity.Notification;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.notification.repository.NotificationRepository;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.domain.user.status.UserStatus;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.sse.SseEmitterStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private SseEmitterStorage sseEmitterStorage;

    @InjectMocks
    private NotificationService notificationService;

    private User activeUser(Long id) {
        User user = User.create("test@test.com", "pw", "홍길동", "길동이", 25, Role.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Notification notification(User user) {
        Notification n = Notification.create(user, NotificationType.DISPUTE_RESOLVED, "제목", "메시지", 10L);
        ReflectionTestUtils.setField(n, "id", 1L);
        return n;
    }

    // ─────────────────────────────────────────────
    // createNotification
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("알림 생성 성공 - SSE emitter 없어도 DB에 저장")
    void createNotification_성공_SSE없음() {
        User user = activeUser(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(sseEmitterStorage.get(1L)).willReturn(null);

        notificationService.createNotification(1L, NotificationType.DISPUTE_RESOLVED, "제목", "메시지", 10L);

        then(notificationRepository).should().save(any(Notification.class));
        then(sseEmitterStorage).should(never()).remove(any());
    }

    @Test
    @DisplayName("알림 생성 성공 - SSE emitter 있으면 실시간 전송")
    void createNotification_성공_SSE전송() throws Exception {
        User user = activeUser(1L);
        SseEmitter emitter = mock(SseEmitter.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(sseEmitterStorage.get(1L)).willReturn(emitter);

        notificationService.createNotification(1L, NotificationType.DISPUTE_RESOLVED, "제목", "메시지", 10L);

        then(notificationRepository).should().save(any(Notification.class));
        then(emitter).should().send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("알림 생성 중 SSE 전송 실패 시 emitter 제거")
    void createNotification_SSE오류시_emitter제거() throws Exception {
        User user = activeUser(1L);
        SseEmitter emitter = mock(SseEmitter.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(sseEmitterStorage.get(1L)).willReturn(emitter);
        doThrow(new IOException("연결 끊김")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        notificationService.createNotification(1L, NotificationType.DISPUTE_RESOLVED, "제목", "메시지", 10L);

        then(sseEmitterStorage).should().remove(1L);
    }

    @Test
    @DisplayName("알림 생성 실패 - 유저 없음")
    void createNotification_유저없음_예외() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                notificationService.createNotification(99L, NotificationType.DISPUTE_RESOLVED, "제목", "메시지", 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // ─────────────────────────────────────────────
    // markAsRead
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("알림 읽음 처리 성공")
    void markAsRead_성공() {
        User user = activeUser(1L);
        Notification n = notification(user);
        given(notificationRepository.findByIdWithUser(1L)).willReturn(Optional.of(n));

        notificationService.markAsRead(1L, 1L);

        assertThat(n.isRead()).isTrue();
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 존재하지 않는 알림")
    void markAsRead_알림없음_예외() {
        given(notificationRepository.findByIdWithUser(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 다른 유저의 알림")
    void markAsRead_다른유저_예외() {
        User user = activeUser(1L);
        Notification n = notification(user);
        given(notificationRepository.findByIdWithUser(1L)).willReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationService.markAsRead(99L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    // ─────────────────────────────────────────────
    // markAllAsRead
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("전체 알림 읽음 처리 성공")
    void markAllAsRead_성공() {
        notificationService.markAllAsRead(1L);

        then(notificationRepository).should().markAllAsReadByUserId(1L);
    }

    // ─────────────────────────────────────────────
    // createAnnouncementToRole
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("특정 역할 대상 공지 발송 성공")
    void createAnnouncementToRole_특정역할_성공() {
        User user1 = activeUser(1L);
        User user2 = activeUser(2L);
        given(userRepository.findByRoleAndStatus(Role.USER, UserStatus.ACTIVE))
                .willReturn(List.of(user1, user2));

        notificationService.createAnnouncementToRole(Role.USER, NotificationType.DISPUTE_RESOLVED,
                "공지", "내용", null);

        then(notificationRepository).should().saveAll(any());
    }

    @Test
    @DisplayName("전체 유저 공지 발송 성공 (targetRole null)")
    void createAnnouncementToRole_전체발송_성공() {
        User user1 = activeUser(1L);
        given(userRepository.findByStatus(UserStatus.ACTIVE)).willReturn(List.of(user1));

        notificationService.createAnnouncementToRole(null, NotificationType.DISPUTE_RESOLVED,
                "전체 공지", "내용", null);

        then(notificationRepository).should().saveAll(any());
    }

    @Test
    @DisplayName("공지 대상 유저가 없으면 저장 없이 종료")
    void createAnnouncementToRole_대상없음_저장안함() {
        given(userRepository.findByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE))
                .willReturn(List.of());

        notificationService.createAnnouncementToRole(Role.ADMIN, NotificationType.DISPUTE_RESOLVED,
                "공지", "내용", null);

        then(notificationRepository).should(never()).saveAll(any());
    }
}
