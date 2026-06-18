package com.team04.domain.notification.controller;

import com.team04.domain.notification.dto.response.NotificationResponse;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.notification.service.NotificationService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable){

        return ApiResponse.ofSuccess(notificationService.getMyNotifications(userDetails.getUserId(), pageable));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ){
        notificationService.markAsRead(userDetails.getUserId(), notificationId);
        return ApiResponse.ofSuccessWithoutBody();
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAsReadAll(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        notificationService.markAllAsRead(userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails){
        return notificationService.subscribe(userDetails.getUserId());
    }

    //TEST------------------------------------------------------------------------------------------------
    @PostMapping("/test")
    public ApiResponse<Void> testNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.createNotification(
                userDetails.getUserId(),
                NotificationType.MATCH_REQUESTED,
                "테스트 알림",
                "SSE 테스트 메시지",
                1L
        );
        return ApiResponse.ofSuccessWithoutBody();
    }

}
