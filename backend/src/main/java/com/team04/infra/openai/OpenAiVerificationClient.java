package com.team04.infra.openai;

import com.team04.domain.verification.dto.openai.OpenAiVerificationRequest;
import com.team04.domain.verification.dto.openai.OpenAiVerificationResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/** Spring HTTP Interface 기반 OpenAI 검증 API 클라이언트입니다. */
@HttpExchange("/v1")
public interface OpenAiVerificationClient {

    /** OpenAI Chat Completions API에 Structured Output 검증 요청을 전송합니다. */
    @PostExchange("/chat/completions")
    OpenAiVerificationResponse verify(@RequestBody OpenAiVerificationRequest request);
}
