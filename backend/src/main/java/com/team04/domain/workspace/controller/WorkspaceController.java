package com.team04.domain.workspace.controller;

import com.team04.domain.milestone.dto.response.FundUsageResponse;
import com.team04.domain.workspace.dto.request.WorkspaceMessageRequest;
import com.team04.domain.workspace.dto.response.WorkspaceMessageResponse;
import com.team04.domain.workspace.dto.response.WorkspaceResponse;
import com.team04.domain.workspace.service.WorkspaceService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "워크스페이스", description = "프로젝트 워크스페이스 조회, 메시지 송수신, 자금 사용 내역 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    /** 워크스페이스(프로젝트) 정보를 조회합니다. 창작자 또는 결제 성공 후원자만 접근 가능합니다. */
    @Operation(
            summary = "워크스페이스 조회",
            description = "워크스페이스(프로젝트) 정보를 조회합니다. 창작자, 결제 완료 후원자, 관리자만 접근 가능합니다."
    )
    @GetMapping("/{workspaceId}")
    public ApiResponse<WorkspaceResponse> getWorkspace(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ofSuccess(workspaceService.getWorkspace(
                workspaceId,
                userDetails.getUserId(),
                userDetails.getRole()
        ));
    }

    /** 워크스페이스에 메시지를 전송합니다. */
    @Operation(
            summary = "워크스페이스 메시지 전송",
            description = "워크스페이스에 메시지를 전송합니다. 창작자, 결제 완료 후원자, 관리자만 가능합니다."
    )
    @PostMapping("/{workspaceId}/messages")
    public ApiResponse<WorkspaceMessageResponse> sendMessage(
            @PathVariable Long workspaceId,
            @Valid @RequestBody WorkspaceMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ofSuccess(workspaceService.sendMessage(
                workspaceId,
                userDetails.getUserId(),
                userDetails.getRole(),
                request
        ));
    }

    /** 워크스페이스 메시지 목록을 조회합니다. */
    @Operation(
            summary = "워크스페이스 메시지 목록 조회",
            description = "워크스페이스의 메시지 목록을 조회합니다. 창작자, 결제 완료 후원자, 관리자만 접근 가능합니다."
    )
    @GetMapping("/{workspaceId}/messages")
    public ApiResponse<List<WorkspaceMessageResponse>> getMessages(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ofSuccess(workspaceService.getMessages(
                workspaceId,
                userDetails.getUserId(),
                userDetails.getRole()
        ));
    }

    /** 워크스페이스 참여자용 자금 사용 내역을 조회합니다. */
    @Operation(
            summary = "자금 사용 내역 조회",
            description = "프로젝트 자금 사용 내역을 조회합니다. 창작자, 결제 완료 후원자, 관리자만 접근 가능합니다."
    )
    @GetMapping("/{workspaceId}/fund-usage")
    public ApiResponse<List<FundUsageResponse>> getFundUsages(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ofSuccess(workspaceService.getFundUsages(
                workspaceId,
                userDetails.getUserId(),
                userDetails.getRole()
        ));
    }
}
