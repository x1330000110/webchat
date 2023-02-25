package com.socket.client.command.user.impl;

import com.socket.client.command.user.UserChangeHandler;
import com.socket.core.model.ws.WsUser;
import org.springframework.stereotype.Component;

/**
 * 头像变更
 */
@Component
public class Headimg extends UserChangeHandler {
    @Override
    public void invoke(WsUser target, String param) {
        target.setHeadimgurl(param);
    }
}
