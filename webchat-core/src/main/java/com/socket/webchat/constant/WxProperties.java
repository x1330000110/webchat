package com.socket.webchat.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信接口认证配置
 *
 * @since 2021/7/9
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
     * 登录成功跳转API完整路径
     */
    private String redirect;
}
