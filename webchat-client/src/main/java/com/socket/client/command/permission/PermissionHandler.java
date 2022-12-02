package com.socket.client.command.permission;

import com.socket.client.command.CommandHandler;
import com.socket.client.manager.WsGroupMap;
import com.socket.client.manager.WsUserMap;
import com.socket.client.model.WsUser;
import com.socket.webchat.custom.event.PermissionEvent;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * 权限命令处理基础类
 */
public abstract class PermissionHandler implements CommandHandler<PermissionEvent> {
    @Autowired
    protected WsGroupMap groupMap;
    @Autowired
    protected WsUserMap userMap;

    public void invoke(PermissionEvent event) {
        WsUser self = Optional.ofNullable(event.getSelf()).map(userMap::getUser).orElse(null);
        WsUser target = Optional.ofNullable(event.getTarget()).map(userMap::getUser).orElse(null);
        invoke(self, target, event.getParam());
    }

    public abstract <T> void invoke(WsUser self, WsUser target, T param);
}
