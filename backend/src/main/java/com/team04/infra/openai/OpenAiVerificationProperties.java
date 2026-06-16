package com.team04.infra.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** OpenAI 검증 API 호출에 필요한 설정값을 바인딩하는 프로퍼티입니다. */
@ConfigurationProperties(prefix = "openai.verification")
public record OpenAiVerificationProperties(
        String baseUrl,
        String apiKey,
        String model,
        Integer timeoutSeconds
) {

    /** 설정이 비어 있을 때 사용할 OpenAI 기본 엔드포인트를 반환합니다. */
    public String baseUrl() {
        return baseUrl == null ? "https://api.openai.com" : baseUrl;
    }

    /** 설정이 비어 있을 때 사용할 검증 모델명을 반환합니다. */
    public String model() {
        return model == null ? "gpt-4.1-mini" : model;
    }

    /** 설정이 비어 있을 때 사용할 5분 타임아웃 초를 반환합니다. */
    public Integer timeoutSeconds() {
        return timeoutSeconds == null ? 300 : timeoutSeconds;
    }
}
