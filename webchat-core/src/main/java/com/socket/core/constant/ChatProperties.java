package com.socket.core.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@RefreshScope
@Component
@ConfigurationProperties(prefix = "webchat.config")
public class ChatProperties {
    /**
     * 邮箱验证码有效时间（单位：分钟）
     */
    private int emailCodeValidTime = 5;
    /**
     * 微信登录二维码过期时间（单位：秒）
     */
    private long qrCodeExpirationTime = 60;
    /**
     * 每次邮箱验证码发送间隔（单位: 秒）
     */
    private int emailSendingInterval = 60;
    /**
     * 邮箱验证码发送次数上限恢复时间间隔（单位: 小时）
     */
    private int emailLimitSendingInterval = 12;
    /**
     * 聊天记录历史记录同步数量
     */
    private int syncRecordsNums = 25;
    /**
     * 聊天系统10秒内允许发言的次数
     */
    private int frequentSpeechThreshold = 10;
    /**
     * 频繁发言禁言时间（单位：小时）
     */
    private int frequentSpeechMuteTime = 6;
    /**
     * 允许的消息撤回时间（单位：秒）
     */
    private int withdrawTime = 120;
    /**
     * 最大消息长度
     */
    private int maxMessageLength = 300;
    /**
     * 缓存屏蔽列表的时间（单位：小时）
     */
    private long shieldCacheTime = 24;
    /**
     * 最大群组创建数
     */
    private int maxCreateGroupNum = 3;
    /**
     * 最大可设置的入群密码长度
     */
    private int maxGroupPassword = 16;
}
