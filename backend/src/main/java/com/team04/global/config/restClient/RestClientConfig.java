package com.team04.global.config.restClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);  // 3초: 연결 자체가 안 되는 경우
        factory.setReadTimeout(10000);    // 10초: 연결 후 응답 대기

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}