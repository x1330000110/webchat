package com.socket.client.command.group.impl;

import com.socket.client.command.group.GroupHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Exit extends GroupHandler {
    @Override
    public void execute(SysGroupUser user, SysGroup group) {
        WsUser find = userMap.getUser(user.getUid());
        List<WsUser> groupUsers = groupMap.getGroupUsers(user.getGroupId());
        groupUsers.remove(find);
    }
}
