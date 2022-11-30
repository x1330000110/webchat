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
    public void execute(WsUser user, String data) {
        userMap.exit(user, "您已被管理员永久限制登陆");
        userMap.remove(user.getUid());
    }
}
