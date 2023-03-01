package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.client.model.SocketMessage;
import com.socket.client.model.SocketUser;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.command.impl.PermissionEnum;
import com.socket.core.util.Wss;
import org.springframework.stereotype.Component;

/**
 * 撤回消息
 */
@Component
public class Withdraw extends PermissionHandler {
    @Override
    public <T> void invoke(SocketUser self, BaseUser target, T param) {
        String suid = self.getGuid(), tuid = target.getGuid();
        // 构建消息
        SocketMessage message = new SocketMessage((String) param, PermissionEnum.WITHDRAW);
        message.setGuid(suid);
        message.setTarget(tuid);
        // 目标是群组 通知群组撤回此消息
        if (Wss.isGroup(tuid)) {
            message.setData(groupManager.get(tuid));
            groupManager.sendGroup(message);
            return;
        }
        // 通知双方撤回此消息
        message.setData(self);
        userManager.get(tuid).send(message);
        self.send(message);
    }
}
