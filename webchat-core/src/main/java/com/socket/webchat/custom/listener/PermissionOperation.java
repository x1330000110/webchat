package com.socket.webchat.custom.listener;

import com.socket.webchat.model.enums.Command;

/**
 * 权限变动枚举
 */
public enum PermissionOperation implements Command {
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
    ANNOUNCE,
    /**
     * 设置头衔
     */
    ALIAS,
    /**
     * 设置管理员
     */
    ROLE;

    @Override
    public String getName() {
        return getClass().getSimpleName() + "." + name().toLowerCase();
    }
}
