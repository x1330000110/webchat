package com.socket.client.command;

import cn.hutool.core.util.StrUtil;
import com.socket.client.command.group.GroupChangeHandler;
import com.socket.client.command.permiss.PermissHandler;
import com.socket.client.command.user.UserChangeHandler;
import com.socket.webchat.custom.event.GroupChangeEvent;
import com.socket.webchat.custom.event.PermissionEvent;
import com.socket.webchat.custom.event.UserChangeEvent;
import com.socket.webchat.model.command.Command;
import com.socket.webchat.model.command.impl.GroupEnum;
import com.socket.webchat.model.command.impl.PermissEnum;
import com.socket.webchat.model.command.impl.UserEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 全局命令事件处理
 */
@Component
@RequiredArgsConstructor
public class GlobalEventHandler {
    private final Map<String, PermissHandler> permissionHandlers;
    private final Map<String, GroupChangeHandler> groupHandlers;
    private final Map<String, UserChangeHandler> userHandlers;

    /**
     * 群组事件
     */
    @EventListener(GroupChangeEvent.class)
    public void onGroupChange(GroupChangeEvent event) {
        GroupEnum command = event.getOperation();
        groupHandlers.get(key(command)).invoke(event);
    }

    /**
     * 用户事件
     */
    @EventListener(UserChangeEvent.class)
    public void onUserChange(UserChangeEvent event) {
        UserEnum command = event.getOperation();
        userHandlers.get(key(command)).invoke(event);
    }

    /**
     * 权限事件
     */
    @EventListener(PermissionEvent.class)
    public void onPermission(PermissionEvent event) {
        PermissEnum command = event.getOperation();
        permissionHandlers.get(key(command)).invoke(event);
    }

    private <E extends Enum<E> & Command<?>> String key(E command) {
        return StrUtil.toCamelCase(command.name().toLowerCase());
    }
}
