package com.socket.webchat.util;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.support.collections.*;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RedisClient {
    private final RedisOperations<String, Object> operations;
    private final ValueOperations<String, Object> opsvalue;

    public RedisClient(RedisTemplate<String, Object> operations) {
        this.operations = operations;
        this.opsvalue = operations.opsForValue();
        operations.afterPropertiesSet();
    }

    public RedisList<Object> withList(String key) {
        return new DefaultRedisList<>(operations.boundListOps(key));
    }

    public RedisMap<String, Object> withMap(String key) {
        return new DefaultRedisMap<>(operations.boundHashOps(key));
    }

    public RedisSet<Object> withSet(String key) {
        return new DefaultRedisSet<>(operations.boundSetOps(key));
    }

    public RedisZSet<Object> withZset(String key) {
        return new DefaultRedisZSet<>(operations.boundZSetOps(key));
    }

    public boolean setIfAbsent(String key, Object value) {
        return Objects.requireNonNull(opsvalue.setIfAbsent(key, value));
    }

    public boolean setIfAbsent(String key, Object value, long second) {
        return Objects.requireNonNull(opsvalue.setIfAbsent(key, value, second, TimeUnit.SECONDS));
    }

    public boolean setIfPresent(String key, Object value) {
        return Objects.requireNonNull(opsvalue.setIfPresent(key, value));
    }

    public boolean setIfPresent(String key, Object value, long second) {
        return Objects.requireNonNull(opsvalue.setIfPresent(key, value, second, TimeUnit.SECONDS));
    }

    public boolean setExpired(String key, int time) {
        return setExpired(key, time, TimeUnit.SECONDS);
    }

    private boolean setExpired(String key, int time, TimeUnit unit) {
        if (time > 0) {
            return Objects.requireNonNull(operations.expire(key, time, unit));
        }
        remove(key);
        return false;
    }

    public long getExpired(String key) {
        Long obj = operations.getExpire(key, TimeUnit.SECONDS);
        return obj == null ? -2 : obj;
    }

    public <T> T get(String key) {
        //noinspection unchecked
        return (T) opsvalue.get(key);
    }

    public <T> T getOrDefault(String key, T defval) {
        T v = get(key);
        return v == null ? defval : v;
    }

    public long incr(String key, int delta) {
        Long obj = opsvalue.increment(key, delta);
        return obj == null ? delta : obj;
    }

    public long incr(String key, int delta, int second) {
        return incr(key, delta, second, TimeUnit.SECONDS);
    }

    public long incr(String key, int delta, int time, TimeUnit unit) {
        Long obj = opsvalue.increment(key, delta);
        if (time > 0) {
            setExpired(key, time, unit);
        }
        return obj == null ? delta : obj;
    }

    public boolean exist(String key) {
        return Objects.requireNonNull(operations.hasKey(key));
    }

    public boolean remove(String key) {
        return Objects.requireNonNull(operations.delete(key));
    }

    public void set(String key, Object value) {
        set(key, value, -1);
    }

    public void set(String key, Object value, long second) {
        set(key, value, second, TimeUnit.SECONDS);
    }

    public void set(String key, Object value, long second, TimeUnit unit) {
        if (second > 0) {
            opsvalue.set(key, value, second, unit);
        } else {
            remove(key);
        }
    }

    public boolean isEmpty(String key) {
        Object v = get(key);
        return v == null || v instanceof String && ((String) v).isEmpty();
    }
}
