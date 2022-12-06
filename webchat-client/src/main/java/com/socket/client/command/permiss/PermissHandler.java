package com.socket.client.command.permiss;

import com.socket.client.command.CommandHandler;
import com.socket.client.manager.WsGroupMap;
import com.socket.client.manager.WsUserMap;
import com.socket.client.model.WsUser;
import com.socket.webchat.custom.event.PermissionEvent;
import com.socket.webchat.model.BaseUser;
import com.socket.webchat.util.Wss;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * 权限命令处理基础类
 */
public abstract class PermissHandler implements CommandHandler<PermissionEvent> {
    @Autowired
    protected WsGroupMap groupMap;
    @Autowired
    protected WsUserMap userMap;

    public void invoke(PermissionEvent event) {
        WsUser self = Optional.ofNullable(event.getSelf()).map(userMap::getUser).orElse(null);
        // 目标可能是群组
        invoke(self, getBaseUser(event.getTarget()), event.getParam());
    }

    public abstract <T> void invoke(WsUser self, BaseUser target, T param);

    private BaseUser getBaseUser(String guid) {
        // 目标可能为空（如公告）
        if (guid == null) {
            return null;
        }
        return Wss.isGroup(guid) ? groupMap.getGroup(guid) : userMap.getUser(guid);
    }
}
