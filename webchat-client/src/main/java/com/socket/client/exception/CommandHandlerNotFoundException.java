package com.socket.client.exception;

import com.socket.webchat.model.command.Command;

/**
 * 找不到命令执行器异常
 */
public class CommandHandlerNotFoundException extends RuntimeException {
    public CommandHandlerNotFoundException() {
        super();
    }

    public CommandHandlerNotFoundException(Command<?> command) {
        super("找不到指定命令的处理程序：" + command.getName());
    }
}
