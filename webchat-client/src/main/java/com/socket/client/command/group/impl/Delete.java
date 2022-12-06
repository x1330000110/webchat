package com.socket.client.command.group.impl;

import com.socket.client.command.group.GroupChangeHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.command.impl.GroupEnum;
import org.springframework.stereotype.Component;

/**
 * 移除群组用户
 */
@Component
public class Delete extends GroupChangeHandler {
    @Override
    public void invoke(SysGroupUser user, SysGroup group) {
        WsUser find = userMap.get(user.getUid());
        find.send("您已被管理员移除群聊", GroupEnum.DELETE);
    }
}
