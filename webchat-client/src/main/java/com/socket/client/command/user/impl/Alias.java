package com.socket.client.command.user.impl;

import com.socket.client.command.user.UserChangeHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.command.impl.UserEnum;
import org.springframework.stereotype.Component;

/**
 * 设置头衔
 */
@Component
public class Alias extends UserChangeHandler {
    @Override
    public void invoke(WsUser target, String param) {
        userMap.sendAll(param, UserEnum.ALIAS, target);
    }
}
