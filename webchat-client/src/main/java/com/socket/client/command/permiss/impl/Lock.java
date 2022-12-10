package com.socket.client.command.permiss.impl;

import com.socket.client.command.permiss.PermissHandler;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.webchat.model.BaseUser;
import com.socket.webchat.model.command.impl.PermissEnum;
import org.springframework.stereotype.Component;

/**
 * 限制登陆
 */
@Component
public class Lock extends PermissHandler {
    @Override
    public <T> void invoke(WsUser self, BaseUser target, T param) {
        Long time = (Long) param;
        if (time > 0) {
            userMap.exit((WsUser) target, Callback.LOGIN_LIMIT.format(time));
        }
        userMap.sendAll(String.valueOf(time), PermissEnum.LOCK, target);
    }
}
