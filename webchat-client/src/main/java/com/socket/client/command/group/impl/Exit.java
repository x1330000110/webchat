package com.socket.client.command.group.impl;

import com.socket.client.command.group.GroupChangeHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.command.impl.GroupEnum;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 退出群组
 */
@Component
public class Exit extends GroupChangeHandler {
    @Override
    public void invoke(SysGroupUser user, SysGroup group) {
        WsUser find = userMap.get(user.getUid());
        String gid = user.getGid();
        List<WsUser> groupUsers = groupMap.getGroupUsers(gid);
        groupMap.sendGroup(gid, null, GroupEnum.EXIT, user);
        groupUsers.remove(find);
    }
}
