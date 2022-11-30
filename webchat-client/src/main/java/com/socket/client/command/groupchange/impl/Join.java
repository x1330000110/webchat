package com.socket.client.command.groupchange.impl;

import com.socket.client.command.groupchange.GroupChangeHandler;
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
    public void execute(SysGroupUser user, SysGroup group) {
        String uid = user.getUid(), gid = user.getGroupId();
        WsUser wsuser = userMap.getUser(uid);
        groupMap.getGroupUsers(gid).add(wsuser);
        // 发送通知
        groupMap.sendGroup(gid, gid, GroupEnum.JOIN, user);
    }
}
