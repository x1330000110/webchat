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
}
