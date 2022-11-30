package com.socket.client.command.permission.impl;

import com.socket.client.command.permission.PermissionHandler;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.command.impl.PermissionEnum;
import com.socket.webchat.util.Wss;
import org.springframework.stereotype.Component;

@Component
public class Withdraw extends PermissionHandler {
    @Override
    public void execute(ChatRecord record) {
        WsUser self = userMap.getUser(record.getUid());
        // 构建消息
        String target = record.getTarget();
        String mid = record.getMid();
        WsMsg wsmsg = new WsMsg(mid, PermissionEnum.WITHDRAW);
        wsmsg.setUid(self.getUid());
        wsmsg.setTarget(target);
        // 目标是群组 通知群组撤回此消息
        if (Wss.isGroup(target)) {
            wsmsg.setData(groupMap.getGroup(target));
            groupMap.sendGroup(wsmsg);
            return;
        }
        // 通知双方撤回此消息
        wsmsg.setData(self);
        userMap.getUser(target).send(wsmsg);
        self.send(wsmsg);
    }
}
