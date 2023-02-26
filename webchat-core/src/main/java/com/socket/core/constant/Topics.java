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
    String USER_COMMAND = "USER_COMMAND";
    /**
     * 群组命令
     */
    String GROUP_COMMAND = "GROUP_COMMAND";
    /**
     * 权限命令
     */
    String PERMISSION_COMMAND = "PERMISSION_COMMAND";
}
