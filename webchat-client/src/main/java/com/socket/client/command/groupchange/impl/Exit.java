package com.socket.client.command.groupchange.impl;

import com.socket.client.command.groupchange.GroupChangeHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 退出群组
 */
@Component
public class Exit extends GroupChangeHandler {
    @Override
    public void invoke(SysGroupUser user, SysGroup group) {
        WsUser find = userMap.getUser(user.getUid());
        List<WsUser> groupUsers = groupMap.getGroupUsers(user.getGid());
        groupUsers.remove(find);
    }
}
