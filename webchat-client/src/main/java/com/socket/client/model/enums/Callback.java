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
    USER_NOT_FOUND("找不到消息发送的目标用户（可能已注销）"),
    BRUSH_SCREEN("检查到存在刷屏行为，您已被禁言{}"),
    REJECT_EXECUTE("操作被拒绝：权限不足"),
    INVALID_COMMAND("命令不正确"),
    WITHDRAW_FAILURE("超过{}的消息无法撤回"),
    USER_LOGIN("{} 加入聊天室"),
    USER_LOGOUT("{} 退出聊天室"),
    JOIN_INIT("INIT"),
    MUTE_LIMIT("您已被管理员禁言{}"),
    C_MUTE_LIMIT("您已被管理员解除禁言"),
    G_MUTE_LIMIT("{} 已被管理员禁言{}"),
    GC_MUTE_LIMIT("{} 已被管理员解除禁言"),
    LOGIN_LIMIT("您已被管理员限制登陆{}"),
    LIMIT_FOREVER("您已被管理员永久限制登陆"),
    G_LOGIN_LIMIT("{} 已被管理员限制登陆{}"),
    GC_LOGIN_LIMIT("{} 已被管理员解除登录限制"),
    AUTH_ADMIN("您已被所有者任命为管理员"),
    AUTH_USER("您已被所有者取消管理员权限"),
    G_AUTH_ADMIN("{} 已被所有者任命为管理员"),
    G_AUTH_USER("{} 已被所有者取消管理员权限"),
    TARGET_SHIELD("消息未发出，您屏蔽了对方"),
    ALL_MUTE("所有者开启了全员禁言"),
    SELF_SHIELD("消息已发出，但被对方拒收了"),
    SHIELD_USER("您已将 {} 消息屏蔽"),
    SENSITIVE_KEYWORDS("消息包含敏感关键词，请检查后重新发送"),
    CANCEL_SHIELD("您已取消屏蔽 {} 消息");

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

    public String format(WsUser user) {
        return StrUtil.format(message, user.getName());
    }

    public String format(WsUser user, long time) {
        return StrUtil.format(message, user.getName(), Wss.universal(time));
    }

}
