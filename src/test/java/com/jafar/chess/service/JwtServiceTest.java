package com.jafar.chess.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    @Test
    void validateTokenReturnsUserIdAndBlocksRevokedToken() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(false, true);

        JwtService jwtService = new JwtService(redisTemplate);
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);

        String token = jwtService.generateToken("user-123", "user@example.com");

        assertEquals("user-123", jwtService.validateTokenAndGetUserId(token));

        jwtService.revokeToken(token);
        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
        assertNull(jwtService.validateTokenAndGetUserId(token));
    }
}

