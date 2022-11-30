package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.command.impl.PermissionEnum;
import com.socket.webchat.model.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
public class Role extends PermissionHandler {
    @Override
    public void execute(WsUser user, String data) {
        user.setRole(UserRole.of(data));
        userMap.sendAll(PermissionEnum.ROLE, user);
    }
}
