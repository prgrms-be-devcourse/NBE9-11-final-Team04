package com.team04.domain.verification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/** 검증 도메인의 비동기 작업 실행 스레드풀을 설정하는 클래스입니다. */
@Configuration
public class VerificationAsyncConfig {

    /** OpenAI 검증 백그라운드 작업에 사용할 전용 Executor를 생성합니다. */
    @Bean(name = "verificationTaskExecutor")
    public Executor verificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("verification-");
        executor.initialize();
        return executor;
    }
}
