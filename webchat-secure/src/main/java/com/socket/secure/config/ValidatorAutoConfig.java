package com.socket.secure.config;

import com.socket.secure.constant.SecureProperties;
import com.socket.secure.filter.validator.RepeatValidator;
import com.socket.secure.filter.validator.impl.MapRepeatValidator;
import com.socket.secure.filter.validator.impl.RedisRepeatValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.File;

@Configuration
public class ValidatorAutoConfig {
    private final SecureProperties properties;

    public ValidatorAutoConfig(SecureProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RedisTemplate.class)
    public RepeatValidator repeatValidator(StringRedisTemplate template) {
        return new RedisRepeatValidator(template, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RepeatValidator repeatValidator() {
        String tempDir = System.getProperty("java.io.tmpdir");
        return new MapRepeatValidator(new File(tempDir, "MapRepeatDataSerialized"), properties);
    }
}
