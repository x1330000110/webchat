package com.socket.server.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 微信接口认证配置
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties("wx.config")
public class WxProperties {
    /**
     * 公众号唯一ID标识
     */
    private String appid;
    /**
     * 公众号密码凭证
     */
    private String appsecret;
    /**
     * 反向代理地址（可以为空）
     */
    private String proxy;
    /**
     * 登录认证重定向地址
     */
    private String redirect;
}
