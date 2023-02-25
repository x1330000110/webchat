package com.socket.client.command.permission.impl;

import cn.hutool.core.util.StrUtil;
import com.socket.client.command.permission.PermissionHandler;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.command.impl.PermissionEnum;
import com.socket.core.model.socket.SocketUser;
import com.socket.core.util.Wss;
import org.springframework.stereotype.Component;

/**
 * 限制登陆
 */
@Component
public class Lock extends PermissionHandler {
    @Override
    public <T> void invoke(SocketUser self, BaseUser target, T param) {
        Long time = (Long) param;
        if (time > 0) {
            userManager.exit((SocketUser) target, StrUtil.format("您已被管理员限制登陆{}", Wss.universal(time)));
        }
        userManager.sendAll(String.valueOf(time), PermissionEnum.LOCK, target);
    }
}
