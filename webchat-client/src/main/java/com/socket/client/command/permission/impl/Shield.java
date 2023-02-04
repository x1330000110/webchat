package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.BaseUser;
import com.socket.webchat.model.command.impl.PermissEnum;
import org.springframework.stereotype.Component;

/**
 * 屏蔽
 */
@Component
public class Shield extends PermissionHandler {
    @Override
    public <T> void invoke(WsUser self, BaseUser target, T param) {
        self.send(null, PermissEnum.SHIELD, target);
    }
}
