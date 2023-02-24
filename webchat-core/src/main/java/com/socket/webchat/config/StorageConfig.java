package com.socket.webchat.config;

import com.socket.webchat.constant.properties.LanzouProperties;
import com.socket.webchat.custom.storage.ResourceStorage;
import com.socket.webchat.custom.storage.ftp.FTPConfig;
import com.socket.webchat.custom.storage.ftp.FTPResourceStorage;
import com.socket.webchat.request.LanzouCloudRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 资源储存自动装配
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StorageConfig {
    private final LanzouProperties properties;
    private final FTPConfig config;

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "ftp.client", name = "host")
    public ResourceStorage ftpResourceStorage() {
        log.info("装载资源储存服务：{}", FTPResourceStorage.class.getName());
        return new FTPResourceStorage(config);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "lanzou.config", name = "phpdisk-info")
    public ResourceStorage lanzouCloudRequest() {
        log.info("装载资源储存服务：{}", LanzouCloudRequest.class.getName());
        LanzouCloudRequest request = new LanzouCloudRequest(properties);
        request.afterPropertiesSet();
        return request;
    }
}
