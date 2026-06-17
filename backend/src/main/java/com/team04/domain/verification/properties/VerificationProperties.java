package com.team04.domain.verification.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/** application.yml의 검증 도메인 설정값을 바인딩하는 프로퍼티입니다. */
@Component
@ConfigurationProperties(prefix = "verification")
public record VerificationProperties(
        List<String> forbiddenKeywords
) {

    /** 금칙어 목록이 없을 때도 안전하게 순회할 수 있는 목록을 반환합니다. */
    public List<String> forbiddenKeywords() {
        return forbiddenKeywords == null ? List.of() : forbiddenKeywords;
    }
}
