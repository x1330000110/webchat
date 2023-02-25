package com.socket.client.command.user;

import com.socket.client.command.CommandHandler;
import com.socket.client.manager.SocketUserMap;
import com.socket.core.model.command.topic.UserChangeTopic;
import com.socket.core.model.ws.WsUser;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 用户信息处理基础类
 */
public abstract class UserChangeHandler implements CommandHandler<UserChangeTopic> {
    @Autowired
    protected SocketUserMap userMap;

    public void invoke(UserChangeTopic event) {
        this.invoke(userMap.get(event.getTarget()), event.getParam());
    }

    public abstract void invoke(WsUser target, String param);
}
