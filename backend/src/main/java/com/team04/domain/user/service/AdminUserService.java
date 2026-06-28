package com.team04.domain.user.service;

import com.team04.domain.user.dto.request.UserRoleRequest;
import com.team04.domain.user.dto.request.UserStatusRequest;
import com.team04.domain.user.dto.response.AdminUserResponse;
import com.team04.domain.user.dto.response.UserStatsResponse;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.status.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserService userService;

    // 회원 목록 조회
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(UserStatus status, Role role, Pageable pageable) {
        return userService.getUsers(status, role, pageable);
    }

    // 계정 정지/복구
    @Transactional
    public void updateUserStatus(Long userId, UserStatusRequest request) {
        userService.updateUserStatus(userId, request);
    }

    // 권한 변경
    @Transactional
    public void updateUserRole(Long userId, UserRoleRequest request) {
        userService.updateUserRole(userId, request);
    }

    // 강제 탈퇴
    @Transactional
    public void forceWithdraw(Long userId) {
        userService.forceWithdraw(userId);
    }

    // 회원 현황 집계
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats() {
        return userService.getUserStats();
    }
}