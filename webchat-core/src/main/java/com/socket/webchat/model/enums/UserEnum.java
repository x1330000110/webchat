package com.socket.webchat.model.enums;

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
    HEADIMG
}
