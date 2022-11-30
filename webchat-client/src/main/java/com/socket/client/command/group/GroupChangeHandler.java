package com.socket.client.command.group;

import com.socket.client.command.CommandHandler;
import com.socket.client.manager.WsGroupMap;
import com.socket.client.manager.WsUserMap;
import com.socket.webchat.custom.event.GroupChangeEvent;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 群组命令处理基础类
 */
public abstract class GroupChangeHandler implements CommandHandler<GroupChangeEvent> {
    @Autowired
    protected WsGroupMap groupMap;
    @Autowired
    protected WsUserMap userMap;

    public void execute(GroupChangeEvent event) {
        execute(event.getUser(), event.getGroup());
    }

    public abstract void execute(SysGroupUser user, SysGroup group);
}
