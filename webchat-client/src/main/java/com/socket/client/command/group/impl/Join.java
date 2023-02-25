package com.socket.client.command.group.impl;

import com.socket.client.command.group.GroupChangeHandler;
import com.socket.core.model.command.impl.GroupEnum;
import com.socket.core.model.po.SysGroup;
import com.socket.core.model.po.SysGroupUser;
import com.socket.core.model.socket.SocketUser;
import org.springframework.stereotype.Component;

/**
 * 加入群组
 */
@Component
public class Join extends GroupChangeHandler {
    @Override
    public void invoke(SysGroupUser user, SysGroup group) {
        String guid = user.getUid(), gid = user.getGid();
        SocketUser target = userManager.get(guid);
        groupManager.getGroupUsers(gid).add(target);
        // 发送通知
        groupManager.sendGroup(gid, gid, GroupEnum.JOIN, user);
    }
}
