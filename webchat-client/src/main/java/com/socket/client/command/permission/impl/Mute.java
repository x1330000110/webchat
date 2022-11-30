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
    public void execute(WsUser user, String data) {
        userMap.sendAll(data, PermissionEnum.MUTE, user);
    }
}