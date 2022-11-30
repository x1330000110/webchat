package com.socket.client.command.user.impl;

import com.socket.client.command.user.UserHandler;
import com.socket.client.model.WsUser;
import org.springframework.stereotype.Component;

/**
 * 昵称变更
 */
@Component
public class Name extends UserHandler {
    @Override
    public void execute(WsUser user, String data) {
        user.setName(data);
    }
}
