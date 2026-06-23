package com.team04.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OtpRepository {

    private final RedisTemplate<String,String> redisTemplate;

    private static final String PREFIX = "OTP:";
    private static final String VERIFIED_PREFIX = "OTP:VERIFIED:";
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);

    public void save(String email, String token, Duration ttl){
        redisTemplate.opsForValue().set(PREFIX + email, token, ttl);
    }

    public Optional<String> find(String email){
        return Optional.ofNullable(redisTemplate.opsForValue().get(PREFIX + email));
    }

    public void delete(String email){
        redisTemplate.delete(PREFIX + email);
    }

    public void saveVerified(String email) {
        redisTemplate.opsForValue().set(VERIFIED_PREFIX + email, "1", VERIFIED_TTL);
    }

    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(VERIFIED_PREFIX + email));
    }

    public void deleteVerified(String email) {
        redisTemplate.delete(VERIFIED_PREFIX + email);
    }
}
