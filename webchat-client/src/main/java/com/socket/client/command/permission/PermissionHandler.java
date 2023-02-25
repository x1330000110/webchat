package com.socket.client.command.permission;

import com.socket.client.command.CommandHandler;
import com.socket.client.manager.SocketGroupMap;
import com.socket.client.manager.SocketUserMap;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.command.topic.PermissionTopic;
import com.socket.core.model.ws.WsUser;
import com.socket.core.util.Wss;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * 权限命令处理基础类
 */
public abstract class PermissionHandler implements CommandHandler<PermissionTopic> {
    @Autowired
    protected SocketGroupMap groupMap;
    @Autowired
    protected SocketUserMap userMap;

    public void invoke(PermissionTopic event) {
        WsUser self = Optional.ofNullable(event.getSelf()).map(userMap::get).orElse(null);
        this.invoke(self, getBaseUser(event.getTarget()), event.getParam());
    }

    public abstract <T> void invoke(WsUser self, BaseUser target, T param);

    private BaseUser getBaseUser(String guid) {
        // 目标可能为空（如公告）
        if (guid == null) {
            return null;
        }
        return Wss.isGroup(guid) ? groupMap.get(guid) : userMap.get(guid);
    }
}
