package com.socket.client.model;

import cn.hutool.core.annotation.PropIgnore;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.socket.client.model.enums.OnlineState;
import com.socket.secure.util.AES;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.command.Command;
import com.socket.webchat.model.command.impl.CommandEnum;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
    private final List<Session> wss = new CopyOnWriteArrayList<>();
    /**
     * Http Session
     */
    private HttpSession hs;
    /**
     * Shiro Subject
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
        return Objects.equals(target.getGuid(), choose);
    }

    /**
     * 解密消息
     */
    public WsMsg decrypt(String message) {
        WsMsg wsmsg = JSONUtil.toBean(AES.decrypt(message, hs), WsMsg.class);
        wsmsg.setGuid(getGuid());
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
     * @param command  消息类型
     */
    public void send(String callback, Command<?> command) {
        this.send(callback, command, null);
    }

    /**
     * 发送回调通知
     *
     * @param callback 回调消息
     * @param command  消息类型
     * @param data     额外数据
     */
    public void send(String callback, Command<?> command, Object data) {
        this.send(new WsMsg(callback, command, data), false);
    }

    @SneakyThrows(IOException.class)
    private void send(WsMsg wsmsg, boolean async) {
        if (isOnline()) {
            Supplier<String> supplier = () -> AES.encrypt(JSONUtil.toJsonStr(wsmsg), hs);
            List<Session> collect = wss.stream().filter(Session::isOpen).collect(Collectors.toList());
            for (Session session : collect) {
                if (async) {
                    session.getAsyncRemote().sendText(supplier.get());
                } else {
                    session.getBasicRemote().sendText(supplier.get());
                }
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
        // 不同的http session
        if (differentSession(hs)) {
            this.wss.clear();
            this.hs = hs;
            this.subject = subject;
        }
        // 初始化
        this.wss.add(ws);
        this.platform = (String) hs.getAttribute(Constants.PLATFORM);
        this.ip = (String) hs.getAttribute(Constants.IP);
        this.online = OnlineState.ONLINE;
    }

    /**
     * 判断HTTP Session是否与现存的不同（用于网络断开重连）
     *
     * @param session http session
     * @return 是否不同
     */
    public boolean differentSession(HttpSession session) {
        return hs == null || !Objects.equals(hs.getId(), session.getId());
    }

    /**
     * 清除登录数据
     *
     * @param reason 原因（强制退出时填写）
     */
    public void logout(String reason) {
        // 清除ws会话
        if (!wss.isEmpty()) {
            Predicate<Session> test = ws -> reason != null || !ws.isOpen();
            wss.stream().filter(test).forEach(ws -> closeWs(ws, reason));
            wss.removeIf(test);
        }
        // 清除hs会话
        if (reason != null && subject != null) {
            subject.logout();
            this.subject = null;
            this.hs = null;
        }
        // 清除登录状态
        if (wss.isEmpty()) {
            this.online = null;
        }
    }

    private void closeWs(Session session, String reason) {
        try {
            session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, reason));
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }

    /**
     * 发送拒绝消息
     *
     * @param reason 原因
     * @param wsmsg  消息
     */
    public void reject(String reason, WsMsg wsmsg) {
        this.send(reason, CommandEnum.WARNING);
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
