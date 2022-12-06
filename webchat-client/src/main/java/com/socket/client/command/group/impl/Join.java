package com.socket.client.command.group.impl;

import com.socket.client.command.group.GroupChangeHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.command.impl.GroupEnum;
import org.springframework.stereotype.Component;

/**
 * 加入群组
 */
@Component
public class Join extends GroupChangeHandler {
    @Override
    public void invoke(SysGroupUser user, SysGroup group) {
        String guid = user.getUid(), gid = user.getGid();
        WsUser wsuser = userMap.get(guid);
        groupMap.getGroupUsers(gid).add(wsuser);
        // 发送通知
        groupMap.sendGroup(gid, gid, GroupEnum.JOIN, user);
    }
}
