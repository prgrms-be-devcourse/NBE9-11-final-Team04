package com.team04.domain.notification.service;

import com.team04.domain.notification.dto.response.NotificationResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int BATCH_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEmitterStorage sseEmitterStorage;

    @Transactional(readOnly = true)
    public Slice<NotificationResponse> getMyNotifications(Long userId, Pageable pageable){
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::new);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId){

        Notification notification = notificationRepository.findByIdWithUser(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!userId.equals(notification.getUser().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        notification.read();
    }

    @Transactional
    public void markAllAsRead(Long userId)
    {
        notificationRepository.markAllAsReadByUserId(userId);
    }


    public void createNotification(Long userId, NotificationType type, String title, String message, Long referenceId){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.create(user, type, title, message, referenceId);
        notificationRepository.save(notification);

        SseEmitter emitter = sseEmitterStorage.get(userId);
        if(emitter != null){
            try{
                emitter.send(SseEmitter.event().name("notification").data(new NotificationResponse(notification)));
            } catch (IOException e) {
                sseEmitterStorage.remove(userId);
            }
        }
    }


    public void createAnnouncementToRole(Role targetRole, NotificationType type,
                                         String title, String message, Long referenceId) {
        List<User> targets = (targetRole == null)
                ? userRepository.findByStatus(UserStatus.ACTIVE)
                : userRepository.findByRoleAndStatus(targetRole, UserStatus.ACTIVE);

        if (targets.isEmpty()) {
            log.warn("[Notification] 공지 대상 사용자가 없습니다. targetRole={}", targetRole);
            return;
        }

        List<List<User>> batches = partition(targets, BATCH_SIZE);
        for (List<User> batch : batches) {
            List<Notification> notifications = batch.stream()
                    .map(user -> Notification.create(user, type, title, message, referenceId))
                    .toList();

            notificationRepository.saveAll(notifications);

            for (Notification notification : notifications) {
                Long userId = notification.getUser().getId();
                SseEmitter emitter = sseEmitterStorage.get(userId);
                if (emitter != null) {
                    try {
                        emitter.send(SseEmitter.event().name("notification").data(new NotificationResponse(notification)));
                    } catch (IOException e) {
                        sseEmitterStorage.remove(userId);
                    }
                }
            }
        }
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }


    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(3 * 60 * 1000L);

        SseEmitter old = sseEmitterStorage.get(userId);
        if (old != null) old.complete();

        sseEmitterStorage.add(userId, emitter);

        emitter.onCompletion(() -> sseEmitterStorage.remove(userId));
        emitter.onTimeout(() -> sseEmitterStorage.remove(userId));
        emitter.onError((e) -> sseEmitterStorage.remove(userId));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            sseEmitterStorage.remove(userId);
        }

        return emitter;
    }


}
