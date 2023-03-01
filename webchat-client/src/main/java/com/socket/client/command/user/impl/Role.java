package com.socket.client.command.user.impl;

import com.socket.client.command.user.UserChangeHandler;
import com.socket.client.model.SocketUser;
import com.socket.core.model.command.impl.UserEnum;
import com.socket.core.model.enums.UserRole;
import com.socket.core.util.Enums;
import org.springframework.stereotype.Component;

/**
 * 设置管理员
 */
@Component
public class Role extends UserChangeHandler {
    @Override
    public void invoke(SocketUser target, String param) {
        target.setRole(Enums.of(UserRole.class, param));
        userManager.sendAll(UserEnum.ROLE, target);
    }
}
