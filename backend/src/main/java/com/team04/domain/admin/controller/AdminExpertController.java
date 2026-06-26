package com.team04.domain.admin.controller;

import com.team04.domain.expert.dto.response.AdminExpertAppealSummaryResponse;
import com.team04.domain.expert.dto.response.AdminExpertSuspendedResponse;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.repository.ExpertAppealRepository;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.expert.service.AdminExpertService;
import com.team04.domain.expert.service.ExpertVerificationService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.global.storage.AppealStorageClient;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;

@RestController
@RequestMapping("/admin/experts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminExpertController {

    private final AdminExpertService adminExpertService;

    /* 격리된 전문가 목록 조회 API */
    @GetMapping("/suspended")
    public ResponseEntity<ApiResponse<Page<AdminExpertSuspendedResponse>>> getSuspendedExperts(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ofSuccess(
                adminExpertService.getSuspendedExperts(pageable)
        ));
    }

    /* 소명 자료 목록 조회 API */
    @GetMapping("/{expertProfileId}/appeals")
    public ResponseEntity<ApiResponse<List<AdminExpertAppealSummaryResponse>>> getAppeals(
            @PathVariable Long expertProfileId
    ) {
        return ResponseEntity.ok(ApiResponse.ofSuccess(
                adminExpertService.getAppeals(expertProfileId)
        ));
    }

    /* 계정 복구 API */
    @PostMapping("/{expertProfileId}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreExpert(
            @PathVariable Long expertProfileId
    ) {
        adminExpertService.restoreExpert(expertProfileId);
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody());
    }

    /* 권한 강등 API */
    @PostMapping("/{expertProfileId}/demote")
    public ResponseEntity<ApiResponse<Void>> demoteExpert(
            @PathVariable Long expertProfileId
    ) {
        adminExpertService.demoteExpert(expertProfileId);
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody());
    }
}