package com.socket.client.model.enums;

import cn.hutool.core.lang.EnumItem;

/**
 * 在线状态
 */
public enum OnlineState implements EnumItem<OnlineState> {
    /**
     * 在线
     */
    ONLINE,
    /**
     * 繁忙（切出聊天页面）
     */
    BUSY,
    /**
     * 离开（10分钟内无页面操作）
     */
    SUSPEND;

    @Override
    public int intVal() {
        return ordinal();
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
