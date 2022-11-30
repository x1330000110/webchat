package com.socket.client.command;

import org.springframework.context.ApplicationEvent;

/**
 * 命令处理接口
 */
public interface CommandHandler<E extends ApplicationEvent> {
    /**
     * 执行命令
     *
     * @param command 事件
     */
    void execute(E command);
}
