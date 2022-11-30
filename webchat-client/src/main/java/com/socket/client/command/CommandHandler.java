package com.socket.client.command;

import com.socket.webchat.model.command.Command;

/**
 * 命令执行器
 */
public interface CommandHandler<T extends Command<?>> {
    /**
     * 执行命令
     *
     * @param command 命令
     */
    void execute(T command);
}
