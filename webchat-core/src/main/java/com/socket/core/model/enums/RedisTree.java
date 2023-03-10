package com.socket.core.model.enums;

import com.socket.core.util.Enums;

/**
 * Redis目录树
 */
public enum RedisTree {
    /**
     * 记录发送的邮箱和对应验证码
     */
    EMAIL,
    /**
     * 记录所有发送次数上限的邮箱信息
     */
    EMAIL_LIMIT,
    /**
     * 记录所有发送间隔太短的邮箱信息
     */
    EMAIL_TEMP,
    /**
     * 微信UUID轮询标识
     */
    WXUUID,
    /**
     * 未读消息标记
     */
    UNREAD,
    /**
     * 公告信息
     */
    ANNOUNCE,
    /**
     * 所有者设置
     */
    SETTING,
    /**
     * 资源URL缓存
     */
    RESOURCE_URL,
    /**
     * 禁言标记
     */
    MUTE,
    /**
     * 限制登录标记
     */
    LOCK,
    /**
     * 发言标记
     */
    SPEAK,
    /**
     * 屏蔽列表
     */
    SHIELD,
    /**
     * 认证信息
     */
    AUTH;

    private final String dir;

    RedisTree() {
        this.dir = Enums.key(this);
    }

    public String get() {
        return dir;
    }

    public String concat(String end) {
        return dir + ":" + end;
    }
}
