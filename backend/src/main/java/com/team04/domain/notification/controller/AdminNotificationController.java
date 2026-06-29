package com.team04.domain.notification.controller;

import com.team04.domain.notification.dto.request.AnnouncementRequest;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.notification.service.NotificationService;
import com.team04.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 - 알림", description = "관리자 공지 알림 발송 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "공지 알림 발송", description = "특정 역할(targetRole) 또는 전체 사용자(null)에게 공지 알림을 발송합니다.")
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
