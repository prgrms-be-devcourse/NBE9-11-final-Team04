package com.team04.domain.expert.controller;

import com.team04.domain.expert.dto.request.ExpertProfileRequest;
import com.team04.domain.expert.dto.request.ExpertVerifyRequest;
import com.team04.domain.expert.dto.response.ExpertProfileResponse;
import com.team04.domain.expert.dto.response.ExpertVerifyResponse;
import com.team04.domain.expert.service.ExpertProfileService;
import com.team04.domain.expert.service.ExpertVerifyService;
import com.team04.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/experts")
@RequiredArgsConstructor
public class ExpertController {

    private final ExpertProfileService expertProfileService;
    private final ExpertVerifyService expertVerifyService;


    /* 전문가 프로필 등록 API */
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> registerProfile(
            // TODO 인증 정상 동작 이후 @AuthenticationPrincipal로 변경 필요
            // Headers에 key : "X-User-Id" , 1과 같은 id 값을 value로 사용
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ExpertProfileRequest request
    ) {
        ExpertProfileResponse response = expertProfileService.registerProfile(userId, request);
        return ResponseEntity.status(201).body(ApiResponse.ofSuccess(response));
    }


    /* 전문가 프로필 조회 API */
    @GetMapping("/{expertId}")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> getProfile(
            @PathVariable Long expertId
    ) {
        ExpertProfileResponse response = expertProfileService.getProfile(expertId);
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }


    /* 전문가 프로필 검증 API */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<ExpertVerifyResponse>> verify(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ExpertVerifyRequest request
            ) {
        ExpertVerifyResponse response = expertVerifyService.verify(userId, request);
        return ResponseEntity.status(201).body(ApiResponse.ofSuccess(response));
    }

}

