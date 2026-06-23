package com.team04.domain.notification.controller;

import com.team04.domain.notification.dto.request.AnnouncementRequest;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.notification.service.NotificationService;
import com.team04.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ApiResponse<Void> sendAnnouncement(@Valid @RequestBody AnnouncementRequest request) {
        notificationService.createAnnouncementToRole(
                request.targetRole(),
                NotificationType.ANNOUNCEMENT,
                request.title(),
                request.message(),
                null
        );
        return ApiResponse.ofSuccessWithoutBody();
    }
}
