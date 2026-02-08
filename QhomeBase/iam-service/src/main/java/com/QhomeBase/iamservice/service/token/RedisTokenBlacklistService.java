package com.QhomeBase.iamservice.service.token;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisTokenBlacklistService implements TokenBlacklistService {
    private static final String PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;
    @Override
    public boolean isBlacklisted(String jti) {
        return redisTemplate.hasKey(PREFIX + jti);
    }

    @Override
    public void blacklist(String jti, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                PREFIX + jti,
                "1",
                Duration.ofSeconds(ttlSeconds)
        );
    }
}
