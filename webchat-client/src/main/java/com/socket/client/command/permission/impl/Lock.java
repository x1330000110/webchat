package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.webchat.model.command.impl.PermissionEnum;
import org.springframework.stereotype.Component;

@Component
public class Lock extends PermissionHandler {
    @Override
    public void execute(WsUser user, String data) {
        userMap.exit(user, Callback.LOGIN_LIMIT.format(Long.parseLong(data)));
        userMap.sendAll(data, PermissionEnum.LOCK, user);
    }
}
