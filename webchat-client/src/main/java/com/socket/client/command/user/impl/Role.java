package com.socket.client.command.user.impl;

import com.socket.client.command.user.UserChangeHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.command.impl.UserEnum;
import com.socket.webchat.model.enums.UserRole;
import com.socket.webchat.util.Wss;
import org.springframework.stereotype.Component;

/**
 * 设置管理员
 */
@Component
public class Role extends UserChangeHandler {
    @Override
    public void invoke(WsUser target, String param) {
        target.setRole(Wss.enumOf(UserRole.class, param));
        userMap.sendAll(UserEnum.ROLE, target);
    }
}
