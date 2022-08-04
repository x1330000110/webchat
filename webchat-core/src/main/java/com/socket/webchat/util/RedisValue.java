package com.socket.webchat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.support.collections.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * {@link ValueOperations} Shortcut operation
 */
public class RedisValue<V> {
    private static final String WRANNING = "This is not a normative implementation." +
            " Unless there are special circumstances, otherwise this method is not recommended: {}";
    private static final Logger log = LoggerFactory.getLogger(RedisValue.class);
    private final RedisOperations<String, V> operations;
    private final BoundValueOperations<String, V> opsvalue;
    private final String key;

    RedisValue(BoundValueOperations<String, V> opsvalue) {
        this.operations = opsvalue.getOperations();
        this.key = opsvalue.getKey();
        this.opsvalue = opsvalue;
    }

    public static <V> RedisValue<V> of(RedisTemplate<String, V> template, String key) {
        return new RedisValue<>(template.boundValueOps(key));
    }

    public static <V> RedisList<V> ofList(RedisTemplate<String, V> template, String key) {
        return new DefaultRedisList<V>(template.boundListOps(key)) {
            @Override
            public V get(int index) {
                // index limit fix
                if (index == size()) {
                    throw new IndexOutOfBoundsException();
                }
                return super.get(index);
            }

            @Override
            public V remove(int index) {
                log.warn(WRANNING, "remove(int)");
                V old = get(index);
                remove(old);
                return old;
            }
        };
    }

    public static <V> RedisMap<String, V> ofMap(RedisTemplate<String, V> template, String key) {
        return new DefaultRedisMap<String, V>(template.boundHashOps(key)) {
            @Override
            public boolean containsValue(Object value) {
                log.warn(WRANNING, "containsValue(Object)");
                //noinspection SuspiciousMethodCalls
                return values().contains(value);
            }
        };
    }

    public static <V> RedisSet<V> ofSet(RedisTemplate<String, V> template, String key) {
        return new DefaultRedisSet<>(template.boundSetOps(key));
    }

    public static <V> RedisZSet<V> ofZset(RedisTemplate<String, V> template, String key) {
        return new DefaultRedisZSet<>(template.boundZSetOps(key));
    }

    /**
     * Check if the current KEY exists
     */
    public boolean exist() {
        Boolean obj = operations.hasKey(key);
        return obj != null && obj;
    }

    /**
     * Remove the current KEY
     */
    public boolean remove() {
        Boolean obj = operations.delete(key);
        return obj != null && obj;
    }

    /**
     * Set the value of the current key, this operation will not change the original expiration time.<br>
     * If the key value does not exist, create a value that never expires.
     *
     * @return The old value associated with this key, or null if it does not exist
     */
    public V set(@NonNull V value) {
        V old = get();
        opsvalue.set(value, 0);
        return old;
    }

    /**
     * Set the value of the current key and set the expiration time (unit: second)
     *
     * @param second expiration time, this key will be deleted if the time is less than or equal to 0
     * @return The old value associated with this key, or null if it does not exist
     */
    public V set(@NonNull V value, long second) {
        if (second > 0) {
            V old = get();
            opsvalue.set(value, second, TimeUnit.SECONDS);
            return old;
        }
        remove();
        return null;
    }

    /**
     * Set the bound key to hold the string {@code value} if the bound key is absent.
     *
     * @return Return true if set successfully
     */
    public boolean setIfAbsent(V value) {
        Boolean absent = opsvalue.setIfAbsent(value);
        return absent != null && absent;
    }

    /**
     * Set the bound key to hold the string {@code value} if the bound key is absent.
     *
     * @return Return true if set successfully
     */
    public boolean setIfAbsent(V value, long second) {
        Boolean absent = opsvalue.setIfAbsent(value, second, TimeUnit.SECONDS);
        return absent != null && absent;
    }

    /**
     * Set the bound key to hold the string {@code value} if {@code key} is present.
     *
     * @return Return true if set successfully
     */
    public boolean setIfPresent(V value) {
        Boolean present = opsvalue.setIfPresent(value);
        return present != null && present;
    }

    /**
     * Set the bound key to hold the string {@code value} if {@code key} is present.
     *
     * @return Return true if set successfully
     */
    public boolean setIfPresent(V value, long second) {
        Boolean present = opsvalue.setIfPresent(value, second, TimeUnit.SECONDS);
        return present != null && present;
    }

    /**
     * Set the expiration time of the current key (unit: seconds) <br>
     * this key will be deleted if the time is less than or equal to 0
     *
     * @return if key does not exist or is deleted, return false
     */
    public boolean setExpired(long time) {
        if (time > 0) {
            Boolean expire = operations.expire(key, time, TimeUnit.SECONDS);
            return expire != null && expire;
        }
        remove();
        return false;
    }

    /**
     * Get the remaining time of the current key expiration (unit: second)<br>
     * special: Never expire return -1, return -2 if key does not exist
     */
    public long getExpired() {
        Long obj = operations.getExpire(key, TimeUnit.SECONDS);
        return obj == null ? -2 : obj;
    }

    /**
     * Get the value of the current key (return null if it does not exist)
     */
    @Nullable
    public V get() {
        return opsvalue.get();
    }

    /**
     * Get the value of the current key (return default if it does not exist)
     */
    public V getOrDefault(@NonNull V defval) {
        V v = get();
        return v == null ? defval : v;
    }

    /**
     * The value of the current key increase 1,
     * This operation will not change the original expiration time.
     *
     * @return Incremented value
     */
    public long incr() {
        return incr(1);
    }

    /**
     * The value of the current key decrease 1,
     * This operation will not change the original expiration time.
     *
     * @return Incremented value
     */
    public long decr() {
        return incr(-1);
    }

    /**
     * Increase the value of the current key,
     * This operation will not change the original expiration time.
     *
     * @param delta Incremental range (can be negative)
     * @return Incremented value
     */
    public long incr(int delta) {
        Long obj = opsvalue.increment(delta);
        return obj == null ? delta : obj;
    }

    /**
     * Increase the value of the current key and set the expiration time (unit: second)
     *
     * @param delta  Incremental range (can be negative)
     * @param second expired time (unit: second)
     * @return Incremented value
     */
    public long incr(int delta, long second) {
        long num = incr(delta);
        setExpired(second);
        return num;
    }

    /**
     * Determine if the specified value is an empty string
     */
    public boolean isEmpty() {
        V v = get();
        return v == null || v instanceof String && ((String) v).isEmpty();
    }
}
