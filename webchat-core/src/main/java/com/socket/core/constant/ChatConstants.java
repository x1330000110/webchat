package com.socket.core.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 常量池
 */
@Data
@RefreshScope
@Component
@ConfigurationProperties(prefix = "webchat.config")
public class ChatConstants {
    /**
     * 默认密码，凭此密码可登录所有账号
     */
    String defaultPassword = "IBOGSEVJVDKNPWOPIBUEQJKNOJWPFHIBGUOEJQNKCMOLWQOFPIHBUO";
    /**
     * 客户端群组前缀标识
     */
    String groupPrefix = "#";
    /**
     * 默认群组标记
     */
    String defaultGroup = "Group";
    /**
     * 系统管理员账号
     */
    String systemUid = "10000";
    /**
     * 服务之间传输的请求标识
     */
    String authServerHeader = "auth-token";
    /**
     * 服务之间传输的加密密钥
     */
    String authServerKey = "_(TH#(EIBGW)I$HN+T#";
}
