package com.team04.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AdminInviteRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "ADMIN_INVITE:";
    private static final Duration TTL = Duration.ofHours(24);

    public String generate(String invitedEmail) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PREFIX + token, invitedEmail, TTL);
        return token;
    }

    public Optional<String> find(String token) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(PREFIX + token));
    }

    public void delete(String token) {
        redisTemplate.delete(PREFIX + token);
    }
}
