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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final BusinessRegistrationService businessRegistrationService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(
            @AuthenticationPrincipal
            CustomUserDetails userDetails){
        return ApiResponse.ofSuccess(userService.getMe(userDetails.getUserId()));
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserUpdateRequest request){
        return ApiResponse.ofSuccess(userService.updateMe(userDetails.getUserId(), request));
    }

    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PasswordChangeRequest request){
        userService.changePassword(userDetails.getUserId(), request);
        return ApiResponse.ofSuccessWithoutBody();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails){
        userService.withdraw(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/business")
    public ApiResponse<BusinessRegistrationResponse> registerBusiness(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid BusinessRegistrationRequest request
    ) {
        return ApiResponse.ofSuccess(businessRegistrationService.register(userDetails.getUserId(), request));
    }


}
