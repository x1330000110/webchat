package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.command.impl.PermissionEnum;
import com.socket.core.model.ws.WsMsg;
import com.socket.core.model.ws.WsUser;
import com.socket.core.util.Wss;
import org.springframework.stereotype.Component;

/**
 * 撤回消息
 */
@Component
public class Withdraw extends PermissionHandler {
    @Override
    public <T> void invoke(WsUser self, BaseUser target, T param) {
        String suid = self.getGuid(), tuid = target.getGuid();
        // 构建消息
        WsMsg wsmsg = new WsMsg((String) param, PermissionEnum.WITHDRAW);
        wsmsg.setGuid(suid);
        wsmsg.setTarget(tuid);
        // 目标是群组 通知群组撤回此消息
        if (Wss.isGroup(tuid)) {
            wsmsg.setData(groupMap.get(tuid));
            groupMap.sendGroup(wsmsg);
            return;
        }
        // 通知双方撤回此消息
        wsmsg.setData(self);
        userMap.get(tuid).send(wsmsg);
        self.send(wsmsg);
    }
}
