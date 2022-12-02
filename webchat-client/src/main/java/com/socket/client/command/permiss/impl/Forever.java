package com.socket.client.command.permiss.impl;

import com.socket.client.command.permiss.PermissionHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.BaseUser;
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
