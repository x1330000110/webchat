package com.socket.webchat.model.enums;

/**
 * 公告相关key常量
 *
 * @date 2022/6/26
 */
public enum Announce {
    /**
     * 公告内容
     */
    content,
    /**
     * 散列签名
     */
    digest,
    /**
     * 时间戳
     */
    time;

    public String string() {
        return name().toLowerCase();
    }
}
