package com.team04.global.util;

import com.team04.domain.idea.dto.request.DraftMilestoneRequest;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** 임시저장 마일스톤 JSON 문자열 변환을 담당합니다. */
@Component
@RequiredArgsConstructor
public final class IdeaDraftMilestoneConverter {

    private final ObjectMapper objectMapper;

    public String join(List<DraftMilestoneRequest> milestones) {
        if (milestones == null || milestones.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(milestones);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    public List<DraftMilestoneRequest> parse(String milestones) {
        if (milestones == null || milestones.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(milestones, new TypeReference<>() {});
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}