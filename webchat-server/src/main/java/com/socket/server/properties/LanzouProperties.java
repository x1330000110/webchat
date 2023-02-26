package com.socket.server.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 蓝奏云登录凭证（Cookie）
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties("lanzou.config")
public class LanzouProperties {
    /**
     * 用户ID
     */
    private String ylogin;
    /**
     * 密钥
     */
    private String phpdiskInfo;
}
