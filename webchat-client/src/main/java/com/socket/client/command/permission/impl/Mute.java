package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.command.impl.PermissionEnum;
import org.springframework.stereotype.Component;

/**
 * 禁言
 */
@Component
public class Mute extends PermissionHandler {
    @Override
    public <T> void invoke(WsUser self, WsUser target, T param) {
        Long time = (Long) param;
        userMap.sendAll(time, PermissionEnum.MUTE, target);
    }
}
