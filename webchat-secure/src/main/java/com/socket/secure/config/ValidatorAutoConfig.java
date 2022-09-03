package com.socket.secure.config;

import com.socket.secure.constant.SecureProperties;
import com.socket.secure.filter.validator.RepeatValidator;
import com.socket.secure.filter.validator.impl.MappedRepeatValidator;
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
    public RepeatValidator redisRepeatValidator(StringRedisTemplate template) {
        return new RedisRepeatValidator(template, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RepeatValidator repeatValidator() {
        File cache = new File(System.getProperty("java.io.tmpdir"), "MapRepeatValidatorData");
        return new MappedRepeatValidator(cache, properties.getLinkValidTime(), properties.getMaximumConcurrencyPerSecond());
    }
}
