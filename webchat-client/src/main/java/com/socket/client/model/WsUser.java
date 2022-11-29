package com.socket.client.model;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.socket.client.model.enums.OnlineState;
import com.socket.secure.util.AES;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.enums.Command;
import com.socket.webchat.model.enums.MessageType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.subject.Subject;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

import static javax.websocket.CloseReason.CloseCodes;


/**
 * websocket会话数据
 */
@Slf4j
@NoArgsConstructor
public class WsUser extends SysUser {
    /**
     * WebSocket Session
     */
    private Session ws;
    /**
     * Http Session
     */
    private HttpSession hs;
    /**
     * Shiro SysUser
     */
    private Subject subject;
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
     * 登录平台
     */
    @Getter
    private String platform;
    /**
     * 登录IP
     */
    @Getter
    private String ip;

    /**
     * 构建ws用户信息
     *
     * @param sysUser 用户信息
     */
    public WsUser(SysUser sysUser) {
        BeanUtil.copyProperties(sysUser, this);
    }

    /**
     * 测试当前用户会话是否指定目标用户（在线状态）
     *
     * @param target 用户信息
     * @return 选择返回true
     */
    public boolean chooseTarget(WsUser target) {
        return Objects.equals(target.getUid(), choose);
    }

    /**
     * 解密消息
     */
    public WsMsg decrypt(String message) {
        WsMsg wsmsg = JSONUtil.toBean(AES.decrypt(message, hs), WsMsg.class);
        wsmsg.setUid(getUid());
        return wsmsg;
    }

    /**
     * 将消息发送至目标用户（目标不在线调用此方法没有任何效果）
     *
     * @param wsmsg 消息
     */
    public void send(WsMsg wsmsg) {
        this.send(wsmsg, true);
    }

    /**
     * 发送回调通知
     *
     * @param callback 回调消息
     * @param type     消息类型
     * @date 额外数据
     */
    public void send(String callback, Command<?> type) {
        this.send(callback, type, null);
    }

    /**
     * 发送回调通知
     *
     * @param callback 回调消息
     * @param type     消息类型
     * @param data     额外数据
     * @date 额外数据
     */
    public void send(String callback, Command<?> type, Object data) {
        this.send(new WsMsg(callback, type, data), false);
    }

    @SneakyThrows
    private void send(WsMsg wsmsg, boolean async) {
        if (isOnline() && ws.isOpen()) {
            Supplier<String> supplier = () -> AES.encrypt(JSONUtil.toJsonStr(wsmsg), hs);
            if (async) {
                ws.getAsyncRemote().sendText(supplier.get());
            } else {
                ws.getBasicRemote().sendText(supplier.get());
            }
        }
    }

    /**
     * 设置登录数据
     *
     * @param ws      ws session
     * @param hs      http session
     * @param subject shiro
     */
    public void login(Session ws, HttpSession hs, Subject subject) {
        this.ws = ws;
        this.hs = hs;
        this.subject = subject;
        this.platform = (String) hs.getAttribute(Constants.PLATFORM);
        this.ip = (String) hs.getAttribute(Constants.IP);
        this.online = OnlineState.ONLINE;
    }

    /**
     * 清除登录数据
     *
     * @param reason 原因（强制退出时填写）
     */
    public void logout(String reason) {
        // 始终清除ws会话
        if (ws != null) {
            try {
                ws.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, reason));
            } catch (IOException e) {
                log.warn(e.getMessage());
            }
            this.ws = null;
        }
        // 原因为null为自主退出 无需立即终止session
        if (reason != null && subject != null) {
            subject.logout();
            this.subject = null;
            this.hs = null;
        }
        this.online = null;
    }

    /**
     * 发送拒绝消息
     *
     * @param reason 原因
     * @param wsmsg  消息
     */
    public void reject(String reason, WsMsg wsmsg) {
        this.send(reason, MessageType.WARNING);
        wsmsg.setReject(true);
        this.send(wsmsg);
    }

    /**
     * 判断当前用户是否在线
     */
    public boolean isOnline() {
        return online != null;
    }
}

