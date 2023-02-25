package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.socket.SocketUser;
import org.springframework.stereotype.Component;

/**
 * 永久限制登陆
 */
@Component
public class Forever extends PermissionHandler {
    @Override
    public <T> void invoke(SocketUser self, BaseUser target, T param) {
        userManager.exit((SocketUser) target, "您已被管理员永久限制登陆");
        userManager.remove(target.getGuid());
    }
}
