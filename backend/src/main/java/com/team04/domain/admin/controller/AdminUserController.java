package com.team04.domain.admin.controller;

import com.team04.domain.user.dto.request.UserRoleRequest;
import com.team04.domain.user.dto.request.UserStatusRequest;
import com.team04.domain.user.dto.response.AdminUserResponse;
import com.team04.domain.user.dto.response.UserStatsResponse;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.service.AdminUserService;
import com.team04.domain.user.status.UserStatus;
import com.team04.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /* 회원 목록 조회 API */
    @GetMapping
    public ApiResponse<Page<AdminUserResponse>> getUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(adminUserService.getUsers(status, role, pageable));
    }

    /* 역할별 회원 현황 집계 API */
    @GetMapping("/stats")
    public ApiResponse<UserStatsResponse> getUserStats() {
        return ApiResponse.ofSuccess(adminUserService.getUserStats());
    }

    /* 회원 상태 변경 (계정 정지/복구) API */
    @PatchMapping("/{userId}/status")
    public ApiResponse<Void> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody @Valid UserStatusRequest request
    ) {
        adminUserService.updateUserStatus(userId, request);
        return ApiResponse.ofSuccessWithoutBody();
    }

    /* 권한(역할) 변경 API */
    @PatchMapping("/{userId}/role")
    public ApiResponse<Void> updateUserRole(
            @PathVariable Long userId,
            @RequestBody @Valid UserRoleRequest request
    ) {
        adminUserService.updateUserRole(userId, request);
        return ApiResponse.ofSuccessWithoutBody();
    }

    /* 강제 탈퇴 API */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> forceWithdraw(@PathVariable Long userId) {
        adminUserService.forceWithdraw(userId);
        return ResponseEntity.noContent().build();
    }
}