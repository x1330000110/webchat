package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.command.impl.PermissionEnum;
import org.springframework.stereotype.Component;

/**
 * 设置头衔
 */
@Component
public class Alias extends PermissionHandler {
    @Override
    public <T> void invoke(WsUser self, WsUser target, T param) {
        userMap.sendAll(param, PermissionEnum.ALIAS, target);
    }
}
