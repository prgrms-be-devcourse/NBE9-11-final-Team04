package com.team04.domain.verification.dto.openai;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;

import java.util.List;

/** OpenAI 채팅 완료 응답 중 검증 결과 추출에 필요한 필드만 표현합니다. */
public record OpenAiVerificationResponse(List<Choice> choices) {

    /** 첫 번째 선택지의 content를 반환합니다. */
    public String firstContent() {
        if (choices == null || choices.isEmpty()) {
            throw new CustomException(ErrorCode.AI_RESPONSE_EMPTY);
        }
        Choice choice = choices.get(0);
        if (choice == null || choice.message() == null || choice.message().content() == null) {
            throw new CustomException(ErrorCode.AI_RESPONSE_EMPTY);
        }
        return choice.message().content();
    }

    /** OpenAI 응답 선택지 한 건을 표현합니다. */
    public record Choice(Message message) {
    }

    /** OpenAI 응답 메시지의 본문을 표현합니다. */
    public record Message(String content) {
    }
}
