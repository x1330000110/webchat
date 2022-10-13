package com.socket.webchat.model.enums;

/**
 * 群组信息变动枚举
 */
public enum GroupOperation {
    /**
     * 新增群组
     */
    CREATE,
    /**
     * 移除群组
     */
    DISSOLUTION,
    /**
     * 加入用户
     */
    JOIN,
    /**
     * 移除用户
     */
    DELETE
}
