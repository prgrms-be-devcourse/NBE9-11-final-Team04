package com.team04.domain.dispute.controller;

import com.team04.domain.dispute.dto.request.CreateAppealRequest;
import com.team04.domain.dispute.dto.request.CreateDisputeRequest;
import com.team04.domain.dispute.dto.response.DisputeResponse;
import com.team04.domain.dispute.service.DisputeService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

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

        return ApiResponse.ofSuccess(disputeService.getDispute(userDetails.getUserId(),disputeId));
    }

    @PostMapping("/{disputeId}/appeal")
    public ApiResponse<Void> createDisputeAppeal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long disputeId,
            @RequestBody @Valid CreateAppealRequest request){

        disputeService.createAppeal(disputeId, userDetails.getUserId(), request);

        return ApiResponse.ofSuccessWithoutBody();
    }
}
