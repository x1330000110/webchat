package com.socket.client.command.permission;

import com.socket.client.command.CommandHandler;
import com.socket.client.manager.GroupManager;
import com.socket.client.manager.UserManager;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.command.topic.PermissionTopic;
import com.socket.core.model.socket.SocketUser;
import com.socket.core.util.Wss;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * 权限命令处理基础类
 */
public abstract class PermissionHandler implements CommandHandler<PermissionTopic> {
    @Autowired
    protected GroupManager groupManager;
    @Autowired
    protected UserManager userManager;

    public void invoke(PermissionTopic event) {
        SocketUser self = Optional.ofNullable(event.getSelf()).map(userManager::get).orElse(null);
        this.invoke(self, getBaseUser(event.getTarget()), event.getParam());
    }

    public abstract <T> void invoke(SocketUser self, BaseUser target, T param);

    private BaseUser getBaseUser(String guid) {
        // 目标可能为空（如公告）
        if (guid == null) {
            return null;
        }
        return Wss.isGroup(guid) ? groupManager.get(guid) : userManager.get(guid);
    }
}
