package com.socket.webchat.model.enums;

/**
 * Redis目录树
 *
 * @date 2021/10/27
 */
public enum RedisTree {
    /**
     * 记录发送的邮箱和对应验证码
     */
    EMAIL("email:"),
    /**
     * 记录所有发送次数上限的邮箱信息
     */
    LIMIT_EMAIL("email:limit"),
    /**
     * 记录所有发送间隔太短的邮箱信息
     */
    INTERIM_EMAIL("email:temp"),
    /**
     * 微信UUID轮询标识
     */
    WX_UUID("wxuuid:"),
    /**
     * 未读消息标记
     */
    UNREAD("unread:"),
    /**
     * 公告信息
     */
    ANNOUNCEMENT("announce"),
    /**
     * 临时限制登录
     */
    LOCK("lock:");

    private final String dir;

    RedisTree(String dir) {
        this.dir = dir;
    }

    public String getPath() {
        return dir;
    }

    public String getPath(String end) {
        return dir + end;
    }
}
