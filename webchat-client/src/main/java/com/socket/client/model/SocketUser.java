package com.socket.client.model;

import cn.hutool.core.annotation.PropIgnore;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.socket.client.util.AES;
import com.socket.core.custom.TokenUserManager;
import com.socket.core.model.AuthUser;
import com.socket.core.model.command.Command;
import com.socket.core.model.command.impl.CommandEnum;
import com.socket.core.model.enums.OnlineState;
import com.socket.core.model.po.SysUser;
import com.socket.core.util.Enums;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.yeauty.pojo.Session;

import java.util.Objects;

/**
 * websocket会话数据
 */
@Slf4j
@NoArgsConstructor
public class SocketUser extends SysUser {
    /**
     * 内嵌Token用户管理器
     */
    private TokenUserManager manager;
    /**
     * Ws Session
     */
    private Session session;
    /**
     * 是否在线
     */
    @Getter
    @Setter
    private OnlineState online;
    /**
     * 当前选择的用户UID
     */
    @Setter
    private String choose;
    /**
     * Token令牌
     */
    private String token;
    /**
     * 登录平台
     */
    @Getter
    private String platform;
    /**
     * IP地址（日志使用）
     */
    @PropIgnore
    @Getter
    private String ip;

    /**
     * 构建ws用户信息
     *
     * @param sysUser 用户信息
     */
    public SocketUser(SysUser sysUser) {
        BeanUtil.copyProperties(sysUser, this);
    }

    /**
     * 测试当前用户会话是否指定目标用户（在线状态）
     *
     * @param target 用户信息
     * @return 选择返回true
     */
    public boolean chooseTarget(SocketUser target) {
        return Objects.equals(target.getGuid(), choose);
    }

    /**
     * 解密消息
     */
    public SocketMessage decrypt(String str) {
        AuthUser auth = manager.getTokenUser(token);
        if (auth == null) {
            return null;
        }
        // 解密消息
        String decrypt = AES.decrypt(str, auth.getKey());
        JSONObject json = JSONUtil.parseObj(decrypt);
        SocketMessage message = json.toBean(SocketMessage.class, true);
        message.setType(Enums.of(CommandEnum.class, json.getStr("type")));
        message.setGuid(getGuid());
        return message;
    }

    /**
     * 发送回调通知
     *
     * @param callback 回调消息
     * @param command  消息类型
     * @param data     额外数据
     */
    public void send(String callback, Command<?> command, Object data) {
        this.send(new SocketMessage(callback, command, data));
    }

    /**
     * 发送回调通知
     *
     * @param callback 回调消息
     * @param command  消息类型
     */
    public void send(String callback, Command<?> command) {
        this.send(callback, command, null);
    }

    /**
     * 发送拒绝消息
     *
     * @param reason  原因
     * @param message 消息
     */
    public void reject(String reason, SocketMessage message) {
        this.send(reason, CommandEnum.WARNING);
        message.setReject(true);
        this.send(message);
    }

    /**
     * 设置登录数据
     *
     * @param manager {@link TokenUserManager}
     * @param session Ws会话
     * @param token   令牌
     */
    public void login(TokenUserManager manager, Session session, String token) {
        this.manager = manager;
        this.session = session;
        this.token = token;
        this.online = OnlineState.ONLINE;
    }

    /**
     * 清除登录数据
     *
     * @param reason 原因（强制退出时填写）
     */
    public void logout(String reason) {
        session.close();
    }

    /**
     * 将消息发送至目标用户（目标不在线调用此方法没有任何效果）
     *
     * @param message 消息
     */
    public void send(SocketMessage message) {
        if (isOnline()) {
            AuthUser auth = manager.getTokenUser(token);
            if (auth != null) {
                // 加密消息
                String str = JSONUtil.toJsonStr(message);
                String encrypt = AES.encrypt(str, auth.getKey());
                session.sendText(encrypt);
            }
        }
    }

    /**
     * 判断当前用户是否在线
     */
    public boolean isOnline() {
        return online != null;
    }
}
