package com.team04.domain.verification.service;

import com.team04.domain.verification.dto.openai.AiVerificationStructuredResult;
import com.team04.domain.verification.dto.openai.OpenAiVerificationRequest;
import com.team04.domain.verification.dto.request.VerificationRequest;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.infra.openai.OpenAiVerificationClient;
import com.team04.infra.openai.OpenAiVerificationProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
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

    /** Resilience4j Retry와 Circuit Breaker를 적용해 AI 검증을 수행합니다. */
    @Retry(name = "openAiVerification")
    @CircuitBreaker(name = "openAiVerification")
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

    /** 아이디어 본문과 마일스톤 식별자를 AI 검증 프롬프트로 변환합니다. */
    private String buildPrompt(VerificationRequest request) {
        return "제목: " + request.title() + "\n"
                + "설명: " + request.description() + "\n"
                + "마일스톤 ID 목록: " + request.milestoneIds() + "\n"
                + "decision은 PASS, NEEDS_REVISION, REJECT, PENDING_ADMIN_REVIEW 중 하나입니다.";
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