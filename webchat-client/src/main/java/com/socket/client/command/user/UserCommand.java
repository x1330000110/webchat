package com.socket.client.command.user;

import com.socket.client.command.CommandHandler;
import com.socket.webchat.model.command.impl.UserEnum;

public interface UserCommand extends CommandHandler<UserEnum> {
    /**
     * 执行用户命令
     *
     * @param command 命令
     */
    void execute(UserEnum command);
}
