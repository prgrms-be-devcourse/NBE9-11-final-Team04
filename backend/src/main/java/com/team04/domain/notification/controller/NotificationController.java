package com.team04.domain.notification.controller;

import com.team04.domain.notification.dto.response.NotificationResponse;
import com.team04.domain.notification.service.NotificationService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "알림", description = "알림 조회·읽음 처리 및 SSE 실시간 구독 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회", description = "로그인한 사용자의 알림을 최신순으로 조회합니다. (Slice 페이징)")
    @GetMapping
    public ApiResponse<Slice<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable){

        return ApiResponse.ofSuccess(notificationService.getMyNotifications(userDetails.getUserId(), pageable));
    }

    @Operation(summary = "알림 단건 읽음 처리", description = "지정한 알림을 읽음 상태로 변경합니다.")
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ){
        notificationService.markAsRead(userDetails.getUserId(), notificationId);
        return ApiResponse.ofSuccessWithoutBody();
    }

    @Operation(summary = "알림 전체 읽음 처리", description = "로그인한 사용자의 모든 알림을 읽음 상태로 변경합니다.")
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAsReadAll(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        notificationService.markAllAsRead(userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    @Operation(summary = "SSE 알림 구독", description = "Server-Sent Events로 실시간 알림을 구독합니다. 연결 유지 중 새 알림 발생 시 자동으로 Push됩니다.")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails){
        return notificationService.subscribe(userDetails.getUserId());
    }
}
