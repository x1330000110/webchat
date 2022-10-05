package com.socket.client.model.enums;

import cn.hutool.core.util.StrUtil;
import com.socket.client.model.WsUser;
import com.socket.webchat.util.Wss;
import lombok.Getter;

/**
 * 服务器回调消息枚举
 */
public enum Callback {
    // ------ 安全检查 ---------//
    REPEAT_LOGIN("您的账号已在别处登录"),
    SELF_MUTE("您已被禁言，请稍后再试"),
    USER_NOT_FOUND("找不到消息发送的目标用户（可能已注销）"),
    BRUSH_SCREEN("检查到存在刷屏行为，您已被禁言{}"),
    REJECT_EXECUTE("操作被拒绝：权限不足"),
    INVALID_COMMAND("命令不正确"),
    WITHDRAW_FAILURE("超过{}的消息无法撤回"),
    // --------- 加入/退出 --------//
    USER_LOGIN("{} 加入聊天室"),
    USER_LOGOUT("{} 退出聊天室"),
    JOIN_INIT("INIT"),
    //---- 禁言 ------//
    MUTE_LIMIT("您已被管理员禁言{}"),
    C_MUTE_LIMIT("您已被管理员解除禁言"),
    G_MUTE_LIMIT("{} 已被管理员禁言{}"),
    GC_MUTE_LIMIT("{} 已被管理员解除禁言"),
    //---- 限制登录 ------//
    LOGIN_LIMIT("您已被管理员限制登陆{}"),
    LIMIT_FOREVER("您已被管理员永久限制登陆"),
    G_LOGIN_LIMIT("{} 已被管理员限制登陆{}"),
    GC_LOGIN_LIMIT("{} 已被管理员解除登录限制"),
    // ----- 任命管理员 ------//
    AUTH_ADMIN("您已被所有者任命为管理员"),
    AUTH_USER("您已被所有者取消管理员权限"),
    G_AUTH_ADMIN("{} 已被所有者任命为管理员"),
    G_AUTH_USER("{} 已被所有者取消管理员权限"),
    // ------ 屏蔽和检查 --------//
    TARGET_SHIELD("消息未发出，您屏蔽了对方"),
    SELF_SHIELD("消息已发出，但被对方拒收了"),
    SHIELD_USER("您已将 {} 消息屏蔽"),
    CANCEL_SHIELD("您已取消屏蔽 {} 消息"),
    // 手动设置消息
    MANUAL("{}");

    private final String tips;
    @Getter
    private String reason;

    Callback(String tips) {
        this.tips = tips;
    }

    /**
     * 消息构建器，使用枚举后必须调用此方法<br>
     * 传入{@link Number}时将格式化为通用时间<br>
     * 传入{@link WsUser}时消息指定用户<br>
     * 也可同时传入，但{@link WsUser}必须在前面<br>
     */
    public Callback format(Object... obj) {
        if (obj.length > 1 && obj[0] instanceof WsUser && obj[1] instanceof Number) {
            WsUser user = (WsUser) obj[0];
            long time = ((Number) obj[1]).longValue();
            this.reason = StrUtil.format(tips, user.getName(), Wss.universal(time));
            return this;
        }
        if (obj.length > 0) {
            if (obj[0] instanceof Number) {
                long time = ((Number) obj[0]).longValue();
                this.reason = StrUtil.format(tips, Wss.universal(time));
            } else if (obj[0] instanceof WsUser) {
                WsUser user = (WsUser) obj[0];
                this.reason = StrUtil.format(tips, user.getName());
            } else if (obj[0] instanceof String) {
                this.reason = (String) obj[0];
            }
        } else {
            this.reason = this.tips;
        }
        return this;
    }
}
