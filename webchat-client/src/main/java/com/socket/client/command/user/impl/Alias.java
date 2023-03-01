package com.socket.client.command.user.impl;

import com.socket.client.command.user.UserChangeHandler;
import com.socket.client.model.SocketUser;
import com.socket.core.model.command.impl.UserEnum;
import org.springframework.stereotype.Component;

/**
 * 设置头衔
 */
@Component
public class Alias extends UserChangeHandler {
    @Override
    public void invoke(SocketUser target, String param) {
        target.setAlias(param);
        userManager.sendAll(param, UserEnum.ALIAS, target);
    }
}
