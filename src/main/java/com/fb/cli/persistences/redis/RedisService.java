package com.fb.cli.persistences.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    // ==========================================
    // BASIC KEY-VALUE OPERATIONS
    // ==========================================

    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("[RedisService] Lỗi set key {}: {}", key, e.getMessage(), e);
        }
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.error("[RedisService] Lỗi set key {} kèm TTL: {}", key, e.getMessage(), e);
        }
    }

    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("[RedisService] Lỗi get key {}: {}", key, e.getMessage(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object val = get(key);
        if (val == null) {
            return null;
        }
        if (clazz.isInstance(val)) {
            return (T) val;
        }
        return null;
    }

    public String getString(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("[RedisService] Lỗi getString {}: {}", key, e.getMessage(), e);
            return null;
        }
    }

    public void setString(String key, String value) {
        try {
            stringRedisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("[RedisService] Lỗi setString {}: {}", key, e.getMessage(), e);
        }
    }

    public void setString(String key, String value, long timeout, TimeUnit unit) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.error("[RedisService] Lỗi setString {} kèm TTL: {}", key, e.getMessage(), e);
        }
    }

    public boolean delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            log.error("[RedisService] Lỗi delete key {}: {}", key, e.getMessage(), e);
            return false;
        }
    }

    public long delete(Collection<String> keys) {
        try {
            Long count = redisTemplate.delete(keys);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("[RedisService] Lỗi delete keys: {}", e.getMessage(), e);
            return 0L;
        }
    }

    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("[RedisService] Lỗi hasKey {}: {}", key, e.getMessage(), e);
            return false;
        }
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
        } catch (Exception e) {
            log.error("[RedisService] Lỗi expire key {}: {}", key, e.getMessage(), e);
            return false;
        }
    }

    public long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key);
            return expire != null ? expire : -1L;
        } catch (Exception e) {
            log.error("[RedisService] Lỗi getExpire {}: {}", key, e.getMessage(), e);
            return -1L;
        }
    }

    public long increment(String key, long delta) {
        try {
            Long val = redisTemplate.opsForValue().increment(key, delta);
            return val != null ? val : 0L;
        } catch (Exception e) {
            log.error("[RedisService] Lỗi increment {}: {}", key, e.getMessage(), e);
            return 0L;
        }
    }

    public long decrement(String key, long delta) {
        try {
            Long val = redisTemplate.opsForValue().decrement(key, delta);
            return val != null ? val : 0L;
        } catch (Exception e) {
            log.error("[RedisService] Lỗi decrement {}: {}", key, e.getMessage(), e);
            return 0L;
        }
    }

    // ==========================================
    // HASH OPERATIONS
    // ==========================================

    public void hSet(String key, String hashKey, Object value) {
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
        } catch (Exception e) {
            log.error("[RedisService] Lỗi hSet {} -> {}: {}", key, hashKey, e.getMessage(), e);
        }
    }

    public Object hGet(String key, String hashKey) {
        try {
            return redisTemplate.opsForHash().get(key, hashKey);
        } catch (Exception e) {
            log.error("[RedisService] Lỗi hGet {} -> {}: {}", key, hashKey, e.getMessage(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String hashKey, Class<T> clazz) {
        Object val = hGet(key, hashKey);
        if (val == null) {
            return null;
        }
        if (clazz.isInstance(val)) {
            return (T) val;
        }
        return null;
    }

    public Map<Object, Object> hGetAll(String key) {
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            log.error("[RedisService] Lỗi hGetAll {}: {}", key, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    public void hDelete(String key, Object... hashKeys) {
        try {
            redisTemplate.opsForHash().delete(key, hashKeys);
        } catch (Exception e) {
            log.error("[RedisService] Lỗi hDelete {}: {}", key, e.getMessage(), e);
        }
    }

    public boolean hHasKey(String key, String hashKey) {
        try {
            return redisTemplate.opsForHash().hasKey(key, hashKey);
        } catch (Exception e) {
            log.error("[RedisService] Lỗi hHasKey {} -> {}: {}", key, hashKey, e.getMessage(), e);
            return false;
        }
    }

    // ==========================================
    // SET OPERATIONS
    // ==========================================

    public long sAdd(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("[RedisService] Lỗi sAdd {}: {}", key, e.getMessage(), e);
            return 0L;
        }
    }

    public Set<Object> sMembers(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("[RedisService] Lỗi sMembers {}: {}", key, e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    public boolean sIsMember(String key, Object value) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
        } catch (Exception e) {
            log.error("[RedisService] Lỗi sIsMember {}: {}", key, e.getMessage(), e);
            return false;
        }
    }

    public long sRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("[RedisService] Lỗi sRemove {}: {}", key, e.getMessage(), e);
            return 0L;
        }
    }
}
