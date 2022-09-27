package com.socket.webchat.constant.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信接口认证配置
 */
@Data
@Component
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
     * 授权回调页面域名（请保持与微信后台设置相同）
     */
    private String domainService;
}
