package com.team04.domain.verification.service;

import com.team04.domain.verification.dto.openai.AiVerificationStructuredResult;
import com.team04.domain.verification.dto.openai.OpenAiVerificationRequest;
import com.team04.domain.verification.dto.request.VerificationRequest;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.infra.openai.OpenAiVerificationClient;
import com.team04.infra.openai.OpenAiVerificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/** OpenAI 단일 호출로 프로젝트 검증 결과를 생성하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class OpenAiVerificationService {

    private final OpenAiVerificationClient client;
    private final OpenAiVerificationProperties properties;
    private final ObjectMapper objectMapper;

    /** Spring Retry를 적용해 AI 검증을 수행합니다. */
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000),
            exclude = CustomException.class
    )
    public AiVerificationStructuredResult verify(VerificationRequest request) {
        try {
            var response = client.verify(buildRequest(request));
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new CustomException(ErrorCode.AI_RESPONSE_EMPTY);
            }
            String content = response.firstContent();
            if (content == null || content.isBlank()) {
                throw new CustomException(ErrorCode.AI_RESPONSE_EMPTY);
            }
            return objectMapper.readValue(content, AiVerificationStructuredResult.class);
        } catch (CustomException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }

    /** Structured Output 스키마와 검증 프롬프트를 포함한 OpenAI 요청을 생성합니다. */
    private OpenAiVerificationRequest buildRequest(VerificationRequest request) {
        return new OpenAiVerificationRequest(
                properties.model(),
                List.of(
                        new OpenAiVerificationRequest.Message("system", "프로젝트 검증 담당자입니다. 과대광고, 유사서비스, 마일스톤 구체성을 한 번에 판단하세요."),
                        new OpenAiVerificationRequest.Message("user", buildPrompt(request))
                ),
                new OpenAiVerificationRequest.ResponseFormat(
                        "json_schema",
                        new OpenAiVerificationRequest.JsonSchema(
                                "ai_verification_result",
                                true,
                                buildSchema()
                        )
                )
        );
    }

    /** 아이디어 본문과 마일스톤 상세 정보를 AI 검증 프롬프트로 변환합니다. */
    private String buildPrompt(VerificationRequest request) {
        return "제목: " + request.title() + "\n"
                + "설명: " + request.description() + "\n"
                + "마일스톤 목록: " + formatMilestones(request.milestones()) + "\n"
                + "해당 아이디어와 유사한 서비스가 시장에 존재하는지 판단하라. "
                + "탐지 결과는 참고용이며 단독으로 거절 사유가 되지 않는다.\n"
                + "decision은 PASS, NEEDS_REVISION, REJECT, PENDING_ADMIN_REVIEW 중 하나입니다.";
    }

    /** 마일스톤 목록을 목표, 기대 결과, 예정일, 예치 금액 중심의 문자열로 변환합니다. */
    private String formatMilestones(List<VerificationRequest.MilestoneInfo> milestones) {
        if (milestones == null || milestones.isEmpty()) {
            return "[]";
        }
        return milestones.stream()
                .map(milestone -> "목표=" + milestone.goal()
                        + ", 기대 결과=" + milestone.expectedResult()
                        + ", 예정일=" + milestone.expectedDate()
                        + ", 예치 금액=" + milestone.lockedAmount())
                .toList()
                .toString();
    }

    /** OpenAI Structured Output에서 사용할 JSON 스키마를 생성합니다. */
    private Map<String, Object> buildSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("decision", "checks", "reason"),
                "properties", Map.of(
                        "decision", Map.of("type", "string", "enum", List.of("PASS", "NEEDS_REVISION", "REJECT", "PENDING_ADMIN_REVIEW")),
                        "reason", Map.of("type", "string"),
                        "checks", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "required", List.of("checkCode", "passed", "score", "reason"),
                                        "properties", Map.of(
                                                "checkCode", Map.of("type", "string", "enum", List.of("EXAGGERATED_ADVERTISEMENT", "SIMILAR_SERVICE", "MILESTONE_SPECIFICITY")),
                                                "passed", Map.of("type", "boolean"),
                                                "score", Map.of("type", "integer", "minimum", 0, "maximum", 20),
                                                "reason", Map.of("type", "string")
                                        )
                                )
                        )
                )
        );
    }
}