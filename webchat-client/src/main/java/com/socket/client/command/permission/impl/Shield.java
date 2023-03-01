package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.client.model.SocketUser;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.command.impl.PermissionEnum;
import org.springframework.stereotype.Component;

/**
 * 屏蔽
 */
@Component
public class Shield extends PermissionHandler {
    @Override
    public <T> void invoke(SocketUser self, BaseUser target, T param) {
        self.send(null, PermissionEnum.SHIELD, target);
    }
}
