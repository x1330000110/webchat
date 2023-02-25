package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.command.impl.PermissionEnum;
import com.socket.core.model.socket.SocketUser;
import org.springframework.stereotype.Component;

/**
 * 禁言
 */
@Component
public class Mute extends PermissionHandler {
    @Override
    public <T> void invoke(SocketUser self, BaseUser target, T param) {
        Long time = (Long) param;
        userManager.sendAll(String.valueOf(time), PermissionEnum.MUTE, target);
    }
}
