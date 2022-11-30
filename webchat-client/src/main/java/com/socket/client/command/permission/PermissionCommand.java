package com.socket.client.command.permission;

import com.socket.client.command.CommandHandler;
import com.socket.webchat.model.command.impl.PermissionEnum;

public interface PermissionCommand extends CommandHandler<PermissionEnum> {
    /**
     * 执行权限命令
     *
     * @param command 命令
     */
    void execute(PermissionEnum command);
}
