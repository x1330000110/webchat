package com.socket.client.command.group.impl;

import com.socket.client.command.group.GroupChangeHandler;
import com.socket.client.model.SocketUser;
import com.socket.core.model.command.impl.GroupEnum;
import com.socket.core.model.po.SysGroup;
import com.socket.core.model.po.SysGroupUser;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 退出群组
 */
@Component
public class Exit extends GroupChangeHandler {
    @Override
    public void invoke(SysGroupUser user, SysGroup group) {
        SocketUser find = userManager.get(user.getUid());
        String gid = user.getGid();
        List<SocketUser> groupUsers = groupManager.getGroupUsers(gid);
        groupManager.sendGroup(gid, null, GroupEnum.EXIT, user);
        groupUsers.remove(find);
    }
}
