package com.socket.client.command.userchange.impl;

import com.socket.client.command.userchange.UserChangeHandler;
import com.socket.client.model.WsUser;
import org.springframework.stereotype.Component;

/**
 * 头像变更
 */
@Component
public class Headimg extends UserChangeHandler {
    @Override
    public void invoke(WsUser user, String data) {
        user.setHeadimgurl(data);
    }
}
