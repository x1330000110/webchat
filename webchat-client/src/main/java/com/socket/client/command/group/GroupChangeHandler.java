package com.socket.client.command.group;

import com.socket.client.command.CommandHandler;
import com.socket.client.manager.SocketGroupMap;
import com.socket.client.manager.SocketUserMap;
import com.socket.webchat.custom.event.GroupChangeEvent;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 群组命令处理基础类
 */
public abstract class GroupChangeHandler implements CommandHandler<GroupChangeEvent> {
    @Autowired
    protected SocketGroupMap groupMap;
    @Autowired
    protected SocketUserMap userMap;

    public void invoke(GroupChangeEvent event) {
        this.invoke(event.getUser(), event.getGroup());
    }

    public abstract void invoke(SysGroupUser user, SysGroup group);
}
