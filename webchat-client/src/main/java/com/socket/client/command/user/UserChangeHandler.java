package com.socket.client.command.user;

import com.socket.client.command.CommandHandler;
import com.socket.client.manager.UserManager;
import com.socket.client.model.SocketUser;
import com.socket.core.model.command.topic.UserChangeTopic;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 用户信息处理基础类
 */
public abstract class UserChangeHandler implements CommandHandler<UserChangeTopic> {
    @Autowired
    protected UserManager userManager;

    public void invoke(UserChangeTopic event) {
        this.invoke(userManager.get(event.getTarget()), event.getParam());
    }

    public abstract void invoke(SocketUser target, String param);
}
