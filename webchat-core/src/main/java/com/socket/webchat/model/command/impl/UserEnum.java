package com.socket.webchat.model.command.impl;

import com.socket.webchat.model.command.Command;

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
    ROLE
}
