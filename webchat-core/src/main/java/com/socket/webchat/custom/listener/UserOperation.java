package com.socket.webchat.custom.listener;

import com.socket.webchat.model.enums.Command;

/**
 * 用户数据更新枚举
 */
public enum UserOperation implements Command<UserOperation> {
    /**
     * 昵称变动
     */
    NAME,
    /**
     * 头像变动
     */
    HEADIMG
}
