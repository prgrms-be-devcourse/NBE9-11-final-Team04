package com.team04.domain.dispute.controller;

import com.team04.domain.dispute.dto.request.CreateAppealRequest;
import com.team04.domain.dispute.dto.request.CreateDisputeRequest;
import com.team04.domain.dispute.dto.response.DisputeResponse;
import com.team04.domain.dispute.service.DisputeService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
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

@RestController
@RequestMapping("/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @GetMapping("/me")
    public ApiResponse<Page<DisputeResponse>> getMyDisputes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ofSuccess(disputeService.getMyDisputes(userDetails.getUserId(), pageable));
    }

    @GetMapping("/received")
    public ApiResponse<Page<DisputeResponse>> getReceivedDisputes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ofSuccess(disputeService.getReceivedDisputes(userDetails.getUserId(), pageable));
    }

    @PostMapping
    public ApiResponse<DisputeResponse> createDispute(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CreateDisputeRequest request){
        return ApiResponse.ofSuccess(disputeService.createDispute(userDetails.getUserId(), request));
    }

    @GetMapping("/{disputeId}")
    public ApiResponse<DisputeResponse> getDispute(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long disputeId){
        return ApiResponse.ofSuccess(disputeService.getDispute(userDetails.getUserId(), disputeId, userDetails.getRole()));
    }

    @PostMapping(value = "/{disputeId}/appeal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> createDisputeAppeal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long disputeId,
            @RequestPart("data") @Valid CreateAppealRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        disputeService.createAppeal(disputeId, userDetails.getUserId(), request, file);

        return ApiResponse.ofSuccessWithoutBody();
    }
}
