package com.team04.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OAuthCsrfRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "OAUTH:CSRF:";
    private static final Duration TTL = Duration.ofMinutes(10);

    public String generate() {
        String state = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PREFIX + state, "1", TTL);
        return state;
    }

    public boolean validateAndDelete(String state) {
        Boolean deleted = redisTemplate.delete(PREFIX + state);
        return Boolean.TRUE.equals(deleted);
    }
}
