package com.socket.webchat.custom.listener;

import com.socket.webchat.model.enums.Command;

/**
 * 群组信息变动枚举
 */
public enum GroupOperation implements Command {
    /**
     * 新增群组
     */
    CREATE,
    /**
     * 解散群组
     */
    DISSOLVE,
    /**
     * 加入群组
     */
    JOIN,
    /**
     * 退出群组
     */
    EXIT,
    /**
     * 移除用户
     */
    DELETE;

    @Override
    public String getName() {
        return getClass().getSimpleName() + "." + name().toLowerCase();
    }
}
