package com.socket.client.command.permiss.impl;

import cn.hutool.core.util.StrUtil;
import com.socket.client.command.permiss.PermissHandler;
import com.socket.client.model.WsUser;
import com.socket.webchat.model.BaseUser;
import com.socket.webchat.model.command.impl.PermissEnum;
import org.springframework.stereotype.Component;

/**
 * 限制登陆
 */
@Component
public class Lock extends PermissHandler {
    @Override
    public <T> void invoke(WsUser self, BaseUser target, T param) {
        Long time = (Long) param;
        if (time > 0) {
            userMap.exit((WsUser) target, StrUtil.format("您已被管理员限制登陆{}", time));
        }
        userMap.sendAll(String.valueOf(time), PermissEnum.LOCK, target);
    }
}
