package com.socket.webchat.custom.listener;

import com.socket.webchat.model.enums.Command;

/**
 * 用户数据更新枚举
 */
public enum UserOperation implements Command {
    /**
     * 昵称变动
     */
    NAME,
    /**
     * 头像变动
     */
    HEADIMG;

    @Override
    public String getName() {
        return getClass().getSimpleName() + "." + name().toLowerCase();
    }
}
