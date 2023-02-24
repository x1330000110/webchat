package com.socket.webchat.config;

import com.socket.webchat.custom.storage.ResourceStorage;
import com.socket.webchat.request.LanzouCloudRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 资源储存自动装配
 */
@Configuration
public class StorageConfig {
    @Bean
    @ConditionalOnMissingBean
    public ResourceStorage resourceStorage(LanzouCloudRequest storage) {
        return storage;
    }
}
