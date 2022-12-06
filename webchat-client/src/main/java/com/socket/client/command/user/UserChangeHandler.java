package com.socket.client.command.user;

import com.socket.client.command.CommandHandler;
import com.socket.client.manager.SocketUserMap;
import com.socket.client.model.WsUser;
import com.socket.webchat.custom.event.UserChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 用户信息处理基础类
 */
public abstract class UserChangeHandler implements CommandHandler<UserChangeEvent> {
    @Autowired
    protected SocketUserMap userMap;

    public void invoke(UserChangeEvent event) {
        invoke(userMap.get(event.getTarget()), event.getParam());
    }

    public abstract void invoke(WsUser target, String param);
}
