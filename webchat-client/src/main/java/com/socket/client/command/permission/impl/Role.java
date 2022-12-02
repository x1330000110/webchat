package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.command.impl.PermissionEnum;
import com.socket.webchat.model.enums.UserRole;
import org.springframework.stereotype.Component;

/**
 * 设置管理员
 */
@Component
public class Role extends PermissionHandler {
    @Override
    public <T> void invoke(WsUser self, WsUser target, T param) {
        target.setRole(UserRole.of((String) param));
        userMap.sendAll(PermissionEnum.ROLE, target);
    }
}
