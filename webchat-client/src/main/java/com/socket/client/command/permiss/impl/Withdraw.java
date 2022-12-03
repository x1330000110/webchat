package com.socket.client.command.permiss.impl;

import com.socket.client.command.permiss.PermissionHandler;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.BaseUser;
import com.socket.webchat.model.command.impl.PermissEnum;
import com.socket.webchat.util.Wss;
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
        WsMsg wsmsg = new WsMsg((String) param, PermissEnum.WITHDRAW);
        wsmsg.setGuid(suid);
        wsmsg.setTarget(tuid);
        // 目标是群组 通知群组撤回此消息
        if (Wss.isGroup(tuid)) {
            wsmsg.setData(groupMap.getGroup(tuid));
            groupMap.sendGroup(wsmsg);
            return;
        }
        // 通知双方撤回此消息
        wsmsg.setData(self);
        userMap.getUser(tuid).send(wsmsg);
        self.send(wsmsg);
    }
}