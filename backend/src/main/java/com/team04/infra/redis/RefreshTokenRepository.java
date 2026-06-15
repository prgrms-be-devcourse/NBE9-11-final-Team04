package com.team04.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final RedisTemplate<String,String> redisTemplate;

    private static final String PREFIX = "RT:";

    public void save(Long userId, String token, Duration ttl){
        redisTemplate.opsForValue().set(PREFIX + userId, token, ttl);
    }

    public Optional<String> find(Long userId){
        return Optional.ofNullable(redisTemplate.opsForValue().get(PREFIX + userId));
    }

    public void delete(Long userId){
        redisTemplate.delete(PREFIX + userId);
    }
}
