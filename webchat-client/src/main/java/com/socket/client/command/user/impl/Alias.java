package com.socket.client.command.user.impl;

import com.socket.client.command.user.UserChangeHandler;
import com.socket.core.model.command.impl.UserEnum;
import com.socket.core.model.ws.WsUser;
import org.springframework.stereotype.Component;

/**
 * 设置头衔
 */
@Component
public class Alias extends UserChangeHandler {
    @Override
    public void invoke(WsUser target, String param) {
        target.setAlias(param);
        userMap.sendAll(param, UserEnum.ALIAS, target);
    }
}
