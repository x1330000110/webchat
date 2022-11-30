package com.socket.client.command.user.impl;

import com.socket.client.command.user.UserChangeHandler;
import com.socket.client.model.WsUser;
import org.springframework.stereotype.Component;

/**
 * 头像变更
 */
@Component
public class Headimg extends UserChangeHandler {
    @Override
    public void execute(WsUser user, String data) {
        user.setHeadimgurl(data);
    }
}
