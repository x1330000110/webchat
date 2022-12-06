package com.socket.client.command.permiss.impl;

import com.socket.client.command.permiss.PermissHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.BaseUser;
import com.socket.webchat.model.command.impl.PermissEnum;
import org.springframework.stereotype.Component;

/**
 * 发布公告
 */
@Component
public class Announce extends PermissHandler {
    @Override
    public <T> void invoke(WsUser self, BaseUser target, T param) {
        userMap.sendAll(param, PermissEnum.ANNOUNCE);
    }
}
