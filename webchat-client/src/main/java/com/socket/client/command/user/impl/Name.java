package com.socket.client.command.user.impl;

import com.socket.client.command.user.UserChangeHandler;
import com.socket.client.model.WsUser;
import org.springframework.stereotype.Component;

/**
 * 昵称变更
 */
@Component
public class Name extends UserChangeHandler {
    @Override
    public void invoke(WsUser target, String param) {
        target.setName(param);
    }
}
