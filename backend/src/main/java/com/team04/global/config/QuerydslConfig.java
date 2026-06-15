package com.team04.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** QueryDSL JPA 쿼리 생성을 위한 공통 설정입니다. */
@Configuration
public class QuerydslConfig {

    /** 애플리케이션 전역에서 사용하는 JPAQueryFactory 빈을 등록합니다. */
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}
