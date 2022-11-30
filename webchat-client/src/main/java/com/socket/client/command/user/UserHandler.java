package com.socket.client.command.user;

import com.socket.client.command.CommandHandler;
import com.socket.client.manager.WsUserMap;
import com.socket.client.model.WsUser;
import com.socket.webchat.custom.event.UserChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 用户信息处理基础类
 */
public abstract class UserHandler implements CommandHandler<UserChangeEvent> {
    @Autowired
    protected WsUserMap userMap;

    public void execute(UserChangeEvent event) {
        execute(userMap.getUser(event.getUid()), event.getData());
    }

    public abstract void execute(WsUser user, String data);
}
