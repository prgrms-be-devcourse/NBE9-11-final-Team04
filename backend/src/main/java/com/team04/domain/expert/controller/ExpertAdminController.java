package com.team04.domain.expert.controller;

import com.team04.domain.expert.dto.response.AdminExpertAppealSummaryResponse;
import com.team04.domain.expert.dto.response.AdminExpertSuspendedResponse;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.repository.ExpertAppealRepository;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.expert.service.ExpertVerificationService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.global.storage.AppealStorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/experts/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ExpertAdminController {

    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertAppealRepository expertAppealRepository;
    private final ExpertVerificationService expertVerificationService;
    private final AppealStorageClient appealStorageClient;

    // 격리 전문가 목록 조회
    @GetMapping("/suspended")
    public ResponseEntity<ApiResponse<Page<AdminExpertSuspendedResponse>>> getSuspendedExperts(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<AdminExpertSuspendedResponse> response = expertProfileRepository
                .findProfilesByStatus(ExpertStatus.SUSPENDED, pageable)
                .map(AdminExpertSuspendedResponse::from);
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }

    @GetMapping("/{expertProfileId}/appeals")
    public ResponseEntity<ApiResponse<List<AdminExpertAppealSummaryResponse>>> getAppeals(
            @PathVariable Long expertProfileId
    ) {
        expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));

        List<AdminExpertAppealSummaryResponse> response = expertAppealRepository
                .findByExpertProfileIdOrderBySubmittedAtDesc(expertProfileId)
                .stream()
                .map(appeal -> AdminExpertAppealSummaryResponse.from(appeal, appealStorageClient))
                .toList();
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }

    // 계정 복구
    @PostMapping("/{expertProfileId}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreExpert(
            @PathVariable Long expertProfileId
    ) {
        expertVerificationService.restoreProfile(expertProfileId);
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody());
    }

    // 권한 강등
    @PostMapping("/{expertProfileId}/demote")
    public ResponseEntity<ApiResponse<Void>> demoteExpert(
            @PathVariable Long expertProfileId
    ) {
        expertVerificationService.demoteToSponsor(expertProfileId);
        return ResponseEntity.ok(ApiResponse.ofSuccessWithoutBody());
    }
}