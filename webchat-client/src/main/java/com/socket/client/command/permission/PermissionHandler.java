package com.socket.client.command.permission;

import com.socket.client.command.CommandHandler;
import com.socket.client.manager.WsGroupMap;
import com.socket.client.manager.WsUserMap;
import com.socket.client.model.WsUser;
import com.socket.webchat.custom.event.PermissionEvent;
import com.socket.webchat.model.ChatRecord;
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

    public void execute(PermissionEvent event) {
        ChatRecord record = event.getRecord();
        // 消息不为空则实现对消息的处理
        if (record != null) {
            execute(record);
            return;
        }
        // 执行常规命令
        WsUser user = Optional.ofNullable(event.getTarget()).map(userMap::getUser).orElse(null);
        execute(user, event.getData());
    }

    public void execute(WsUser user, String data) {
    }

    public void execute(ChatRecord record) {
    }
}
