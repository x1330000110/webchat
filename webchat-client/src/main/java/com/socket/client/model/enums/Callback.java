package com.socket.client.model.enums;

import cn.hutool.core.util.StrUtil;
import com.socket.client.model.WsUser;
import com.socket.webchat.util.Wss;

/**
 * 服务器回调消息枚举
 */
public enum Callback {
    REPEAT_LOGIN("您的账号已在别处登录"),
    SELF_MUTE("您已被禁言，请稍后再试"),
    USER_NOT_FOUND("找不到相关用户（可能已注销）: {}"),
    BRUSH_SCREEN("检查到存在刷屏行为，您已被禁言{}"),
    JOIN_INIT("INIT"),
    LOGIN_LIMIT("您已被管理员限制登陆{}"),
    LIMIT_FOREVER("您已被管理员永久限制登陆"),
    TARGET_SHIELD("消息未发出，您屏蔽了对方"),
    ALL_MUTE("所有者开启了全员禁言"),
    SELF_SHIELD("消息已发出，但被对方拒收了"),
    SENSITIVE_KEYWORDS("消息包含敏感关键词，请检查后重新发送"),
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
