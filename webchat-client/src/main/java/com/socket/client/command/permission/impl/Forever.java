package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.ws.WsUser;
import org.springframework.stereotype.Component;

/**
 * 永久限制登陆
 */
@Component
public class Forever extends PermissionHandler {
    @Override
    public <T> void invoke(WsUser self, BaseUser target, T param) {
        userMap.exit((WsUser) target, "您已被管理员永久限制登陆");
        userMap.remove(target.getGuid());
    }
}
