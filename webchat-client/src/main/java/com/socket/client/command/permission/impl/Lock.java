package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.webchat.model.BaseUser;
import com.socket.webchat.model.command.impl.PermissionEnum;
import org.springframework.stereotype.Component;

/**
 * 限制登陆
 */
@Component
public class Lock extends PermissionHandler {
    @Override
    public <T> void invoke(WsUser self, BaseUser target, T param) {
        Long time = (Long) param;
        userMap.exit((WsUser) target, Callback.LOGIN_LIMIT.format(time));
        userMap.sendAll(time, PermissionEnum.LOCK, target);
    }
}
