package com.socket.client.exception;

import com.socket.webchat.model.command.Command;

/**
 * 无效命令异常
 */
public class InvalidCommandException extends RuntimeException {
    public InvalidCommandException() {
        super();
    }

    public InvalidCommandException(Command<?> command) {
        super("找不到指定命令的执行器：" + command.getName());
    }
}
