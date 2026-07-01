package com.team04.domain.admin.controller;

import com.team04.domain.admin.dto.response.AdminDashboardResponse;
import com.team04.domain.admin.service.AdminDashboardService;
import com.team04.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 - 대시보드", description = "관리자 서비스 현황 통계 대시보드 API")
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(
            summary = "대시보드 통계 조회",
            description = "전체 프로젝트 현황, 회원 현황, 분쟁 현황, 격리 전문가 수를 한 번에 조회합니다."
    )
    @GetMapping
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.ofSuccess(adminDashboardService.getDashboard());
    }
}