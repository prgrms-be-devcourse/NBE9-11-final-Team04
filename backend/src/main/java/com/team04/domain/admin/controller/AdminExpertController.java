package com.team04.domain.admin.controller;

import com.team04.domain.expert.dto.response.AdminExpertAppealSummaryResponse;
import com.team04.domain.expert.dto.response.AdminExpertSuspendedResponse;
import com.team04.domain.expert.service.AdminExpertService;
import com.team04.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자 - 전문가", description = "관리자 전문가 계정 조회, 복구, 강등 API")
@RestController
@RequestMapping("/admin/experts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminExpertController {

    private final AdminExpertService adminExpertService;

    /* 전문가 목록 조회 API (status 미입력 시 전체) */
    @Operation(
            summary = "전문가 목록 조회",
            description = "status 파라미터로 상태별 전문가 목록을 조회합니다. 생략 시 전체 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminExpertSuspendedResponse>>> getExperts(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ofSuccess(
                adminExpertService.getExperts(status, pageable)
        ));
    }

    /* 격리된 전문가 목록 조회 API */
    @Operation(
            summary = "격리 전문가 목록 조회",
            description = "SUSPENDED 상태의 격리된 전문가 목록을 조회합니다."
    )
    @GetMapping("/suspended")
    public ResponseEntity<ApiResponse<Page<AdminExpertSuspendedResponse>>> getSuspendedExperts(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ofSuccess(
                adminExpertService.getSuspendedExperts(pageable)
        ));
    }

    /* 소명 자료 목록 조회 API */
    @Operation(
            summary = "소명 자료 목록 조회",
            description = "특정 전문가의 소명 자료 목록을 제출 시각 최신순으로 조회합니다."
    )
    @GetMapping("/{expertProfileId}/appeals")
    public ResponseEntity<ApiResponse<List<AdminExpertAppealSummaryResponse>>> getAppeals(
            @PathVariable Long expertProfileId
    ) {
        return ResponseEntity.ok(ApiResponse.ofSuccess(
                adminExpertService.getAppeals(expertProfileId)
        ));
    }

    /* 계정 복구 API */
    @Operation(
            summary = "전문가 계정 복구",
            description = "SUSPENDED 상태의 전문가 계정을 ACTIVE로 복구합니다."
    )
    @PostMapping("/{expertProfileId}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreExpert(
            @PathVariable Long expertProfileId
    ) {
        adminExpertService.restoreExpert(expertProfileId);
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody());
    }

    /* 권한 강등 API */
    @Operation(
            summary = "전문가 권한 강등",
            description = "전문가 계정을 USER 역할로 강등하고 상태를 DEMOTED로 변경합니다."
    )
    @PostMapping("/{expertProfileId}/demote")
    public ResponseEntity<ApiResponse<Void>> demoteExpert(
            @PathVariable Long expertProfileId
    ) {
        adminExpertService.demoteExpert(expertProfileId);
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody());
    }

    @Operation(
            summary = "검토 대기 전문가 목록 조회",
            description = "국가자격증 수동 검토 대기 중인 PENDING_VERIFICATION 상태의 전문가 목록을 조회합니다."
    )
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<AdminExpertSuspendedResponse>>> getPendingExperts(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ofSuccess(
                adminExpertService.getPendingExperts(pageable)
        ));
    }

    @Operation(
            summary = "국가자격증 수동 승인",
            description = "관리자가 국가자격증을 검토하고 전문가 자격을 승인합니다. verified=true, ACTIVE 상태로 전환됩니다."
    )
    @PostMapping("/{expertProfileId}/verify")
    public ResponseEntity<ApiResponse<Void>> approvePendingExpert(
            @PathVariable Long expertProfileId
    ) {
        adminExpertService.approvePendingExpert(expertProfileId);
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody());
    }

    @Operation(
            summary = "국가자격증 수동 거절",
            description = "관리자가 국가자격증을 검토하고 자격을 거절합니다. 프로필이 삭제되며 재신청이 가능합니다."
    )
    @PostMapping("/{expertProfileId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectPendingExpert(
            @PathVariable Long expertProfileId
    ) {
        adminExpertService.rejectPendingExpert(expertProfileId);
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody());
    }
}