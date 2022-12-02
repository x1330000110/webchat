package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.client.model.WsUser;
import org.springframework.stereotype.Component;

/**
 * 永久限制登陆
 */
@Component
public class Forever extends PermissionHandler {
    @Override
    public <T> void invoke(WsUser self, WsUser target, T param) {
        userMap.exit(target, "您已被管理员永久限制登陆");
        userMap.remove(target.getUid());
    }
}
