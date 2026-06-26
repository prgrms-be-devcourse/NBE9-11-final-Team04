package com.team04.domain.user.controller;

import com.team04.domain.user.dto.request.UserRoleRequest;
import com.team04.domain.user.dto.request.UserStatusRequest;
import com.team04.domain.user.dto.response.AdminUserResponse;
import com.team04.domain.user.dto.response.UserStatsResponse;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.service.UserService;
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

    private final UserService userService;

    @GetMapping
    public ApiResponse<Page<AdminUserResponse>> getUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ofSuccess(userService.getUsers(status, role, pageable));
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<Void> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody @Valid UserStatusRequest request) {
        userService.updateUserStatus(userId, request);
        return ApiResponse.ofSuccessWithoutBody();
    }

    @GetMapping("/stats")
    public ApiResponse<UserStatsResponse> getUserStats() {
        return ApiResponse.ofSuccess(userService.getUserStats());
    }

    @PatchMapping("/{userId}/role")
    public ApiResponse<Void> updateUserRole(
            @PathVariable Long userId,
            @RequestBody @Valid UserRoleRequest request) {
        userService.updateUserRole(userId, request);
        return ApiResponse.ofSuccessWithoutBody();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> forceWithdraw(@PathVariable Long userId) {
        userService.forceWithdraw(userId);
        return ResponseEntity.noContent().build();
    }
}
