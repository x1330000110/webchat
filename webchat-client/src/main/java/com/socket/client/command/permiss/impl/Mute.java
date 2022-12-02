package com.socket.client.command.permiss.impl;

import com.socket.client.command.permiss.PermissionHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.BaseUser;
import com.socket.webchat.model.command.impl.PermissEnum;
import org.springframework.stereotype.Component;

/**
 * 禁言
 */
@Component
public class Mute extends PermissionHandler {
    @Override
    public <T> void invoke(WsUser self, BaseUser target, T param) {
        Long time = (Long) param;
        userMap.sendAll(time, PermissEnum.MUTE, target);
    }
}
