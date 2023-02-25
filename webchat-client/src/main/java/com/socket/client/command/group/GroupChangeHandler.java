package com.socket.client.command.group;

import com.socket.client.command.CommandHandler;
import com.socket.client.manager.GroupManager;
import com.socket.client.manager.UserManager;
import com.socket.core.model.command.topic.GroupChangeTopic;
import com.socket.core.model.po.SysGroup;
import com.socket.core.model.po.SysGroupUser;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 群组命令处理基础类
 */
public abstract class GroupChangeHandler implements CommandHandler<GroupChangeTopic> {
    @Autowired
    protected GroupManager groupManager;
    @Autowired
    protected UserManager userManager;

    public void invoke(GroupChangeTopic event) {
        this.invoke(event.getUser(), event.getGroup());
    }

    public abstract void invoke(SysGroupUser user, SysGroup group);
}
