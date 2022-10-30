package com.socket.webchat.util;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.support.collections.*;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RedisClient<V> implements InitializingBean {
    private final RedisTemplate<String, V> template;
    private final ValueOperations<String, V> operations;

    public RedisClient(RedisTemplate<String, V> template) {
        this.template = template;
        this.operations = template.opsForValue();
    }

    public RedisList<V> withList(String key) {
        return new DefaultRedisList<>(template.boundListOps(key));
    }

    public RedisMap<String, V> withMap(String key) {
        return new DefaultRedisMap<>(template.boundHashOps(key));
    }

    public RedisSet<V> withSet(String key) {
        return new DefaultRedisSet<>(template.boundSetOps(key));
    }

    public RedisZSet<V> withZset(String key) {
        return new DefaultRedisZSet<>(template.boundZSetOps(key));
    }

    public ValueOperations<String, V> getOperations() {
        return operations;
    }

    public boolean setIfAbsent(String key, V value) {
        return Objects.requireNonNull(operations.setIfAbsent(key, value));
    }

    public boolean setIfAbsent(String key, V value, long second) {
        return Objects.requireNonNull(operations.setIfAbsent(key, value, second, TimeUnit.SECONDS));
    }

    public boolean setIfPresent(String key, V value) {
        return Objects.requireNonNull(operations.setIfPresent(key, value));
    }

    public boolean setIfPresent(String key, V value, long second) {
        return Objects.requireNonNull(operations.setIfPresent(key, value, second, TimeUnit.SECONDS));
    }

    public boolean setExpired(String key, int time) {
        return setExpired(key, time, TimeUnit.SECONDS);
    }

    private boolean setExpired(String key, int time, TimeUnit unit) {
        if (time > 0) {
            return Objects.requireNonNull(template.expire(key, time, unit));
        }
        remove(key);
        return false;
    }

    public long getExpired(String key) {
        Long obj = template.getExpire(key, TimeUnit.SECONDS);
        return obj == null ? -2 : obj;
    }

    public <T> T get(String key) {
        //noinspection unchecked
        return (T) operations.get(key);
    }

    public <T> T getOrDefault(String key, T defval) {
        T v = get(key);
        return v == null ? defval : v;
    }

    public long incr(String key, int delta) {
        Long obj = operations.increment(key, delta);
        return obj == null ? delta : obj;
    }

    public long incr(String key, int delta, int second) {
        return incr(key, delta, second, TimeUnit.SECONDS);
    }

    public long incr(String key, int delta, int time, TimeUnit unit) {
        Long obj = operations.increment(key, delta);
        if (time > 0) {
            setExpired(key, time, unit);
        }
        return obj == null ? delta : obj;
    }

    public boolean exist(String key) {
        return Objects.requireNonNull(template.hasKey(key));
    }

    public boolean remove(String key) {
        return Objects.requireNonNull(template.delete(key));
    }

    public void set(String key, V value) {
        set(key, value, -1);
    }

    public void set(String key, V value, long second) {
        set(key, value, second, TimeUnit.SECONDS);
    }

    public void set(String key, V value, long second, TimeUnit unit) {
        if (second > 0) {
            operations.set(key, value, second, unit);
        } else {
            remove(key);
        }
    }

    public boolean isEmpty(String key) {
        Object v = get(key);
        return v == null || v instanceof String && ((String) v).isEmpty();
    }

    @Override
    public void afterPropertiesSet() {
        template.afterPropertiesSet();
    }
}
