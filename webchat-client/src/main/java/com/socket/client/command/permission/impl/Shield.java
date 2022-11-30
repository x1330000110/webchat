package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.command.impl.PermissionEnum;
import org.springframework.stereotype.Component;

/**
 * 屏蔽
 */
@Component
public class Shield extends PermissionHandler {
    @Override
    public void execute(WsUser user, String data) {
        userMap.sendAll(PermissionEnum.SHIELD, user);
    }
}
