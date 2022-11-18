package com.socket.client.model.enums;

import cn.hutool.core.util.StrUtil;
import com.socket.client.model.WsUser;
import com.socket.webchat.util.Wss;

/**
 * 服务器回调消息枚举
 */
public enum Callback {
    USER_NOT_FOUND("找不到相关用户（可能已注销）: {}"),
    BRUSH_SCREEN("检查到存在刷屏行为，您已被禁言{}"),
    LOGIN_LIMIT("您已被管理员限制登陆{}"),
    GROUP_DISSOLVE("群 {} 已被创建者解散");

    private final String message;

    Callback(String message) {
        this.message = message;
    }

    public String get() {
        return message;
    }

    public String format(long time) {
        return StrUtil.format(message, Wss.universal(time));
    }

    public String format(WsUser user, long time) {
        return StrUtil.format(message, user.getName(), Wss.universal(time));
    }

    public String format(Object... params) {
        return StrUtil.format(message, params);
    }
}
