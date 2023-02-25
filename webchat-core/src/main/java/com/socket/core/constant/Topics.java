package com.socket.core.constant;

/**
 * MQ消息标识集合
 */
public interface Topics {
    /**
     * 聊天记录
     */
    String MESSAGE = "MESSAGE";
    /**
     * 用户信息同步缓存
     */
    String USER_CHANGE_COMMAND = "USER_CHANGE_COMMAND";
    /**
     * 群组命令
     */
    String GROUP_CHANGE_COMMAND = "GROUP_CHANGE_COMMAND";
    /**
     * 权限命令
     */
    String PERMISSION_COMMAND = "PERMISSION_COMMAND";
}
