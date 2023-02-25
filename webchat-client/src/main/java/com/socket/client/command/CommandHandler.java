package com.socket.client.command;

/**
 * MQ命令处理接口
 */
public interface CommandHandler<E> {
    /**
     * 执行命令
     *
     * @param command 事件
     */
    void invoke(E command);
}
