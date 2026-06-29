package com.team04.domain.dispute.controller;

import com.team04.domain.dispute.dto.request.CreateAppealRequest;
import com.team04.domain.dispute.dto.request.CreateDisputeRequest;
import com.team04.domain.dispute.dto.response.DisputeResponse;
import com.team04.domain.dispute.service.DisputeService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "신고/분쟁", description = "신고 접수 · 조회 · 소명 제출 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @Operation(summary = "내가 신고한 분쟁 목록 조회", description = "로그인한 사용자가 직접 신고한 분쟁 목록을 최신순으로 반환합니다.")
    @GetMapping("/me")
    public ApiResponse<Page<DisputeResponse>> getMyDisputes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ofSuccess(disputeService.getMyDisputes(userDetails.getUserId(), pageable));
    }

    @Operation(summary = "나에 대한 신고 목록 조회", description = "로그인한 사용자가 피신고인인 분쟁 목록을 최신순으로 반환합니다.")
    @GetMapping("/received")
    public ApiResponse<Page<DisputeResponse>> getReceivedDisputes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ofSuccess(disputeService.getReceivedDisputes(userDetails.getUserId(), pageable));
    }

    @Operation(summary = "신고 접수", description = "대상 유형(targetType)·대상 ID·피신고 유저 ID·카테고리·사유를 입력하여 신고를 접수합니다.")
    @PostMapping
    public ApiResponse<DisputeResponse> createDispute(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CreateDisputeRequest request){
        return ApiResponse.ofSuccess(disputeService.createDispute(userDetails.getUserId(), request));
    }

    @Operation(summary = "분쟁 상세 조회", description = "신고자 또는 피신고자 본인만 조회할 수 있습니다.")
    @GetMapping("/{disputeId}")
    public ApiResponse<DisputeResponse> getDispute(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "분쟁 ID") @PathVariable Long disputeId){
        return ApiResponse.ofSuccess(disputeService.getDispute(userDetails.getUserId(), disputeId, userDetails.getRole()));
    }

    @Operation(summary = "소명 제출", description = "피신고자가 소명 내용과 증빙 파일(선택)을 제출합니다. multipart/form-data 형식으로 전송합니다.")
    @PostMapping(value = "/{disputeId}/appeal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> createDisputeAppeal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "분쟁 ID") @PathVariable Long disputeId,
            @RequestPart("data") @Valid CreateAppealRequest request,
            @Parameter(description = "증빙 파일 (선택)")
            @RequestPart(value = "file", required = false) MultipartFile file) {

        disputeService.createAppeal(disputeId, userDetails.getUserId(), request, file);

        return ApiResponse.ofSuccessWithoutBody();
    }
}
