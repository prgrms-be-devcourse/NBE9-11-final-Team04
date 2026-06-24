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
        return restClient.get()
                .uri(expertAdminBase() + "/suspended?page={page}&size={size}",
                        pageable.getPageNumber(), pageable.getPageSize())
                .header("Authorization", resolveToken(request))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<ApiResponse<?>>() {});
    }

    @GetMapping("/{expertProfileId}/appeals")
    public ResponseEntity<?> getAppeals(
            HttpServletRequest request,
            @PathVariable Long expertProfileId
    ) {
        return restClient.get()
                .uri(expertAdminBase() + "/{expertProfileId}/appeals", expertProfileId)
                .header("Authorization", resolveToken(request))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<ApiResponse<?>>() {});
    }

    @PostMapping("/{expertProfileId}/restore")
    public ResponseEntity<?> restoreExpert(
            HttpServletRequest request,
            @PathVariable Long expertProfileId
    ) {
        return restClient.post()
                .uri(expertAdminBase() + "/{expertProfileId}/restore", expertProfileId)
                .header("Authorization", resolveToken(request))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<ApiResponse<Void>>() {});
    }

    @PostMapping("/{expertProfileId}/demote")
    public ResponseEntity<?> demoteExpert(
            HttpServletRequest request,
            @PathVariable Long expertProfileId
    ) {
        return restClient.post()
                .uri(expertAdminBase() + "/{expertProfileId}/demote", expertProfileId)
                .header("Authorization", resolveToken(request))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<ApiResponse<Void>>() {});
    }
}