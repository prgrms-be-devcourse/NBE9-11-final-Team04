package com.team04.global.util;

import com.team04.domain.idea.dto.request.CreateMilestoneRequest;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/** 임시저장 마일스톤 JSON 문자열 변환을 담당합니다. */
public final class IdeaDraftMilestoneConverter {

    private static final JsonMapper OBJECT_MAPPER = JsonMapper.builder().build();
    private static final TypeReference<List<CreateMilestoneRequest>> MILESTONE_LIST_TYPE = new TypeReference<>() {
    };

    private IdeaDraftMilestoneConverter() {
    }

    public static String join(List<CreateMilestoneRequest> milestones) {
        if (milestones == null || milestones.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(milestones);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    public static List<CreateMilestoneRequest> parse(String milestones) {
        if (milestones == null || milestones.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(milestones, MILESTONE_LIST_TYPE);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}