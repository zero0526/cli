package com.fb.cli.persistences.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisServiceTest {

    private RedisTemplate<String, Object> redisTemplate;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private ValueOperations<String, String> stringValueOperations;
    private HashOperations<String, Object, Object> hashOperations;
    private RedisService redisService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = Mockito.mock(RedisTemplate.class);
        stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        stringValueOperations = Mockito.mock(ValueOperations.class);
        hashOperations = Mockito.mock(HashOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        redisService = new RedisService(redisTemplate, stringRedisTemplate);
    }

    @Test
    @DisplayName("set và get cơ bản hoạt động đúng")
    void testSetAndGet() {
        when(valueOperations.get("test:key")).thenReturn("test_value");

        redisService.set("test:key", "test_value");
        verify(valueOperations, times(1)).set("test:key", "test_value");

        Object result = redisService.get("test:key");
        assertEquals("test_value", result);

        String typedResult = redisService.get("test:key", String.class);
        assertEquals("test_value", typedResult);
    }

    @Test
    @DisplayName("set với TTL hoạt động đúng")
    void testSetWithTtl() {
        redisService.set("key_ttl", "val", 60, TimeUnit.SECONDS);
        verify(valueOperations, times(1)).set("key_ttl", "val", 60, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("delete và hasKey hoạt động đúng")
    void testDeleteAndHasKey() {
        when(redisTemplate.delete("key_del")).thenReturn(true);
        when(redisTemplate.hasKey("key_del")).thenReturn(true);

        assertTrue(redisService.hasKey("key_del"));
        assertTrue(redisService.delete("key_del"));
    }

    @Test
    @DisplayName("Hash operations (hSet, hGet) hoạt động đúng")
    void testHashOperations() {
        when(hashOperations.get("user:1", "name")).thenReturn("John");

        redisService.hSet("user:1", "name", "John");
        verify(hashOperations, times(1)).put("user:1", "name", "John");

        String name = redisService.hGet("user:1", "name", String.class);
        assertEquals("John", name);
    }

    @Test
    @DisplayName("Increment hoạt động đúng")
    void testIncrement() {
        when(valueOperations.increment("rate:limit", 1L)).thenReturn(5L);

        long count = redisService.increment("rate:limit", 1L);
        assertEquals(5L, count);
    }
}
