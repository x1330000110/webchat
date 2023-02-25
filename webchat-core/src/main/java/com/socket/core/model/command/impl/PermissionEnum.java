package com.socket.core.model.command.impl;

import com.socket.core.model.command.Command;

/**
 * 权限变动枚举
 */
public enum PermissionEnum implements Command<PermissionEnum> {
    /**
     * 屏蔽
     */
    SHIELD,
    /**
     * 撤回消息
     */
    WITHDRAW,
    /**
     * 禁言
     */
    MUTE,
    /**
     * 限制登录
     */
    LOCK,
    /**
     * 永久限制登陆
     */
    FOREVER,
    /**
     * 推送公告
     */
    ANNOUNCE;

    @Override
    public String toString() {
        return getOpenName();
    }
}
