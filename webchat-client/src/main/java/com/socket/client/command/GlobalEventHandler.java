package com.socket.client.command;

import com.socket.client.command.group.GroupHandler;
import com.socket.client.command.permission.PermissionHandler;
import com.socket.client.command.user.UserHandler;
import com.socket.client.exception.CommandHandlerNotFoundException;
import com.socket.webchat.custom.event.GroupChangeEvent;
import com.socket.webchat.custom.event.PermissionEvent;
import com.socket.webchat.custom.event.UserChangeEvent;
import com.socket.webchat.model.command.Command;
import com.socket.webchat.model.command.impl.GroupEnum;
import com.socket.webchat.model.command.impl.PermissionEnum;
import com.socket.webchat.model.command.impl.UserEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 全局命令事件处理
 */
@Component
@RequiredArgsConstructor
public class GlobalEventHandler {
    private final List<PermissionHandler> permissionHandlers;
    private final List<GroupHandler> groupHandlers;
    private final List<UserHandler> userHandlers;

    /**
     * 群组事件
     */
    @EventListener(GroupChangeEvent.class)
    public void onGroupChange(GroupChangeEvent event) {
        GroupEnum command = event.getOperation();
        getHandler(groupHandlers, command).execute(event);
    }

    /**
     * 用户事件
     */
    @EventListener(UserChangeEvent.class)
    public void onUserChange(UserChangeEvent event) {
        UserEnum command = event.getOperation();
        getHandler(userHandlers, command).execute(event);
    }

    /**
     * 权限事件
     */
    @EventListener(PermissionEvent.class)
    public void onPermission(PermissionEvent event) {
        PermissionEnum command = event.getOperation();
        getHandler(permissionHandlers, command).execute(event);
    }

    /**
     * 获取命令执行器
     *
     * @param handlers 执行器集合
     * @param command  命令
     * @return 执行器
     * @throws CommandHandlerNotFoundException 找不到执行器
     */
    private <T extends CommandHandler<?>, C extends Enum<?> & Command<?>> T getHandler(List<T> handlers, C command) {
        return handlers.stream()
                .filter(e -> e.getClass().getSimpleName().equalsIgnoreCase(command.name()))
                .findFirst()
                .orElseThrow(() -> new CommandHandlerNotFoundException(command));
    }
}
