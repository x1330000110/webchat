package com.socket.client.command.group.impl;

import com.socket.client.command.group.GroupChangeHandler;
import com.socket.client.model.SocketUser;
import com.socket.core.model.command.impl.GroupEnum;
import com.socket.core.model.po.SysGroup;
import com.socket.core.model.po.SysGroupUser;
import org.springframework.stereotype.Component;

/**
 * 移除群组用户
 */
@Component
public class Delete extends GroupChangeHandler {
    @Override
    public void invoke(SysGroupUser user, SysGroup group) {
        SocketUser find = userManager.get(user.getUid());
        find.send("您已被管理员移除群聊", GroupEnum.DELETE);
    }
}
