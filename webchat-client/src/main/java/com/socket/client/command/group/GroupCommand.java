package com.socket.client.command.group;

import com.socket.client.command.CommandHandler;
import com.socket.webchat.model.command.impl.GroupEnum;

public interface GroupCommand extends CommandHandler<GroupEnum> {
    /**
     * 执行群组命令
     *
     * @param command 命令
     */
    void execute(GroupEnum command);
}
