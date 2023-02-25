package com.socket.core.model.enums;

import cn.hutool.core.lang.EnumItem;
import com.socket.core.util.Enums;

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
        return Enums.key(this);
    }
}
