package com.socket.secure.filter.validator.impl;

import com.socket.secure.constant.SecureProperties;
import com.socket.secure.filter.validator.RepeatValidator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

/**
 * Repeated request validation based on {@linkplain RedisTemplate}
 *
 * @see RedisTemplate
 * @since 2021.8.2
 */
public class RedisRepeatValidator implements RepeatValidator {
    private final ValueOperations<String, String> operations;
    private final SecureProperties properties;

    public RedisRepeatValidator(StringRedisTemplate template, SecureProperties properties) {
        this.operations = template.opsForValue();
        this.properties = properties;
        log.info("Redis repeat validator is enable");
    }

    @Override
    public boolean isRepeated(long timestamp, String sign) {
        String key = properties.getRedisKeyPrefix().concat(sign);
        int linkValidTime = properties.getLinkValidTime();
        Boolean absent = operations.setIfAbsent(key, String.valueOf(timestamp), linkValidTime, TimeUnit.SECONDS);
        return absent != null && !absent;
    }
}
