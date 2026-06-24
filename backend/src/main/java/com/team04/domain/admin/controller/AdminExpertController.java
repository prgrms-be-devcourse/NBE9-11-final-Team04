package com.team04.domain.admin.controller;

import com.team04.global.response.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/admin/experts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminExpertController {

    private final RestClient restClient;

    @Value("${admin.internal.base-url}")
    private String baseUrl;

    private String expertAdminBase() {
        return baseUrl + "/experts/admin";
    }

    // null 안전하게 Authorization 헤더 추가하는 헬퍼 메서드
    private RestClient.RequestHeadersSpec<?> withAuth(
            RestClient.RequestHeadersSpec<?> spec,
            HttpServletRequest request
    ) {
        String token = resolveToken(request);
        if (token != null) {
            spec = spec.header("Authorization", token);
        }
        return spec;
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer;
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return "Bearer " + cookie.getValue();
                }
            }
        }
        return null;
    }

    @GetMapping("/suspended")
    public ResponseEntity<?> getSuspendedExperts(
            HttpServletRequest request,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return withAuth(
                restClient.get()
                        .uri(expertAdminBase() + "/suspended?page={page}&size={size}",
                                pageable.getPageNumber(), pageable.getPageSize()),
                request
        ).retrieve().toEntity(new ParameterizedTypeReference<ApiResponse<?>>() {});
    }

    @GetMapping("/{expertProfileId}/appeals")
    public ResponseEntity<?> getAppeals(
            HttpServletRequest request,
            @PathVariable Long expertProfileId
    ) {
        return withAuth(
                restClient.get()
                        .uri(expertAdminBase() + "/{expertProfileId}/appeals", expertProfileId),
                request
        ).retrieve().toEntity(new ParameterizedTypeReference<ApiResponse<?>>() {});
    }

    @PostMapping("/{expertProfileId}/restore")
    public ResponseEntity<?> restoreExpert(
            HttpServletRequest request,
            @PathVariable Long expertProfileId
    ) {
        return withAuth(
                restClient.post()
                        .uri(expertAdminBase() + "/{expertProfileId}/restore", expertProfileId),
                request
        ).retrieve().toEntity(new ParameterizedTypeReference<ApiResponse<Void>>() {});
    }

    @PostMapping("/{expertProfileId}/demote")
    public ResponseEntity<?> demoteExpert(
            HttpServletRequest request,
            @PathVariable Long expertProfileId
    ) {
        return withAuth(
                restClient.post()
                        .uri(expertAdminBase() + "/{expertProfileId}/demote", expertProfileId),
                request
        ).retrieve().toEntity(new ParameterizedTypeReference<ApiResponse<Void>>() {});
    }
}