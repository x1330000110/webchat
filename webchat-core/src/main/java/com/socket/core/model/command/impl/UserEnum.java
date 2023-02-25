package com.socket.core.model.command.impl;

import com.socket.core.model.command.Command;

/**
 * 用户数据更新枚举
 */
public enum UserEnum implements Command<UserEnum> {
    /**
     * 昵称变动
     */
    NAME,
    /**
     * 头像变动
     */
    HEADIMG,
    /**
     * 设置头衔
     */
    ALIAS,
    /**
     * 设置管理员
     */
    ROLE;

    @Override
    public String toString() {
        return getOpenName();
    }
}
