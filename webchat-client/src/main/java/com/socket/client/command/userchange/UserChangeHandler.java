package com.socket.client.command.userchange;

import com.socket.client.command.CommandHandler;
import com.socket.client.manager.WsUserMap;
import com.socket.client.model.WsUser;
import com.socket.webchat.custom.event.UserChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 用户信息处理基础类
 */
public abstract class UserChangeHandler implements CommandHandler<UserChangeEvent> {
    @Autowired
    protected WsUserMap userMap;

    public void invoke(UserChangeEvent event) {
        invoke(userMap.getUser(event.getTarget()), event.getParam());
    }

    public abstract void invoke(WsUser target, String param);
}
