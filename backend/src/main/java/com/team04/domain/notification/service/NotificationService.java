package com.team04.domain.notification.service;

import com.team04.domain.notification.dto.response.NotificationResponse;
import com.team04.domain.notification.entity.Notification;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.notification.repository.NotificationRepository;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Long userId, Pageable pageable){
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::new);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId){

        Notification notification = notificationRepository.findById(notificationId)
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

    @Transactional
    public void createNotification(Long userId, NotificationType type, String title, String message, Long referenceId){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.create(user, type, title, message, referenceId);
        notificationRepository.save(notification);
    }

    @Transactional
    public void createNotificationsToAdmins(NotificationType type, String title,
                                            String message, Long referenceId) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);

        List<Notification> notifications = admins.stream()
                .map(admin -> Notification.create(admin, type, title, message, referenceId))
                .toList();

        notificationRepository.saveAll(notifications);
    }


}
