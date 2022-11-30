package com.socket.client.command.group.impl;

import com.socket.client.command.group.GroupHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysGroupUser;
import com.socket.webchat.model.command.impl.GroupEnum;
import org.springframework.stereotype.Component;

@Component
public class Delete extends GroupHandler {
    @Override
    public void execute(SysGroupUser user, SysGroup group) {
        WsUser find = userMap.getUser(user.getUid());
        find.send("您已被管理员移除群聊", GroupEnum.DELETE);
    }
}
