package com.team04.domain.user.controller;

import com.team04.domain.businessregistration.dto.request.BusinessRegistrationRequest;
import com.team04.domain.businessregistration.dto.response.BusinessRegistrationResponse;
import com.team04.domain.businessregistration.service.BusinessRegistrationService;
import com.team04.domain.user.dto.request.PasswordChangeRequest;
import com.team04.domain.user.dto.request.UserUpdateRequest;
import com.team04.domain.user.dto.response.UserResponse;
import com.team04.domain.user.service.UserService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "사용자", description = "사용자 정보 조회·수정 및 사업자 등록 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final BusinessRegistrationService businessRegistrationService;

    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 프로필 정보를 반환합니다.")
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(
            @AuthenticationPrincipal
            CustomUserDetails userDetails){
        return ApiResponse.ofSuccess(userService.getMe(userDetails.getUserId()));
    }

    @Operation(summary = "내 정보 수정", description = "닉네임·소개·포트폴리오 URL을 수정합니다.")
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserUpdateRequest request){
        return ApiResponse.ofSuccess(userService.updateMe(userDetails.getUserId(), request));
    }

    @Operation(summary = "프로필 이미지 수정", description = "프로필 이미지를 업로드하여 변경합니다.")
    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> updateProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ofSuccess(userService.updateProfileImage(userDetails.getUserId(), file));
    }

    @Operation(summary = "프로필 이미지 삭제", description = "프로필 이미지를 기본 이미지로 초기화합니다.")
    @DeleteMapping("/me/profile-image")
    public ApiResponse<UserResponse> deleteProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ofSuccess(userService.deleteProfileImage(userDetails.getUserId()));
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 검증 후 새 비밀번호로 변경합니다.")
    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PasswordChangeRequest request){
        userService.changePassword(userDetails.getUserId(), request);
        return ApiResponse.ofSuccessWithoutBody();
    }

    @Operation(summary = "회원 탈퇴", description = "계정을 비활성화합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails){
        userService.withdraw(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 사업자 정보 조회", description = "등록된 사업자 정보를 반환합니다.")
    @GetMapping("/me/business")
    public ApiResponse<BusinessRegistrationResponse> getMyBusiness(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(businessRegistrationService.getMyBusiness(userDetails.getUserId()));
    }

    @Operation(summary = "사업자 등록", description = "사업자등록번호·대표자명·개업일로 국세청 API 진위 확인 후 등록합니다.")
    @PostMapping("/me/business")
    public ApiResponse<BusinessRegistrationResponse> registerBusiness(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid BusinessRegistrationRequest request
    ) {
        return ApiResponse.ofSuccess(businessRegistrationService.register(userDetails.getUserId(), request));
    }

    @Operation(summary = "사업자 정보 삭제", description = "등록된 사업자 정보를 삭제합니다.")
    @DeleteMapping("/me/business")
    public ResponseEntity<Void> deleteMyBusiness(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        businessRegistrationService.deleteMyBusiness(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
