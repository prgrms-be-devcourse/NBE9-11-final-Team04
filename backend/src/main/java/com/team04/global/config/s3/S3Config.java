package com.team04.global.config.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** 운영 환경에서 사용할 AWS S3 클라이언트 빈을 생성하는 설정 클래스입니다. */
@Configuration
@Profile("!local")
public class S3Config {

    @Value("${cloud.aws.s3.region:ap-northeast-2}")
    private String region;

    /** application 설정의 리전을 기준으로 S3Client를 생성합니다. */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}