package com.fb.cli.persistences.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RedisIntegrationTest {

    @Autowired
    private RedisService redisService;

    @Test
    @DisplayName("Kết nối thực tế tới Redis container và thực hiện cache key-value, expire")
    void testRealRedisConnectionAndCache() {
        String testKey = "test:cli:ping";
        String testValue = "hello_redis_" + System.currentTimeMillis();

        redisService.set(testKey, testValue, 30, TimeUnit.SECONDS);

        Object retrieved = redisService.get(testKey);
        assertEquals(testValue, retrieved);

        assertTrue(redisService.hasKey(testKey));
        assertTrue(redisService.getExpire(testKey) > 0);

        redisService.delete(testKey);
        assertFalse(redisService.hasKey(testKey));
    }
}
