package com.socket.client.model;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.json.JSONUtil;
import com.socket.client.model.enums.Callback;
import com.socket.secure.util.AES;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.enums.MessageType;
import com.socket.webchat.model.enums.UserRole;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.subject.Subject;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Objects;

import static javax.websocket.CloseReason.CloseCodes;


/**
 * websocket会话数据
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class WsUser extends SysUser {
    /**
     * WebSocket Session
     */
    @Getter
    private Session session;
    /**
     * Http Session
     */
    private HttpSession httpSession;
    /**
     * Shiro SysUser
     */
    private Subject subject;
    /**
     * 是否在线
     */
    @Getter
    private boolean online;
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
     * 检查当前用户是否是游客
     */
    public boolean isGuest() {
        return getRole() == UserRole.GUEST;
    }

    /**
     * 当前登录的用户是否是管理员
     */
    public boolean isAdmin() {
        return getRole() == UserRole.ADMIN || isOwner();
    }

    /**
     * 检查当前用户是否是所有者
     */
    public boolean isOwner() {
        return getRole() == UserRole.OWNER;
    }

    /**
     * 解密消息
     */
    public WsMsg decrypt(String message) {
        WsMsg wsmsg = JSONUtil.toBean(AES.decrypt(message, httpSession), WsMsg.class);
        wsmsg.setUid(getUid());
        return wsmsg;
    }

    /**
     * 将消息发送至目标用户（目标不在线调用此方法没有任何效果）
     *
     * @param wsmsg 消息
     */
    public void send(WsMsg wsmsg) {
        if (online && session.isOpen()) {
            session.getAsyncRemote().sendText(AES.encrypt(JSONUtil.toJsonStr(wsmsg), httpSession));
        }
    }

    /**
     * 发送回调通知
     *
     * @param callback 回调消息
     * @param type     消息类型
     * @date 额外数据
     */
    public void send(Callback callback, MessageType type, Object data) {
        this.send(new WsMsg(callback, type, data));
    }

    /**
     * 发送回调错误
     *
     * @param callback 回调消息
     * @param type     消息类型
     * @param objs     参数
     */
    public void sendError(Callback callback, MessageType type, Object... objs) {
        this.send(new WsMsg(callback.format(objs), type, null));
    }

    /**
     * 设置登录数据
     *
     * @param session     ws session
     * @param httpSession http session
     * @param platform    登录平台
     */
    public void login(Session session, HttpSession httpSession, String platform) {
        this.session = session;
        this.httpSession = httpSession;
        this.platform = platform;
        this.online = true;
    }

    /**
     * 清除登录数据
     *
     * @param reason 原因（强制退出时填写）
     * @param objs   参数
     */
    public void logout(Callback reason, Object... objs) {
        // 始终清除ws会话
        if (session != null) {
            try {
                String str = Opt.ofNullable(reason).peek(e -> e.format(objs)).map(Callback::getReason).get();
                session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, str));
            } catch (IOException e) {
                log.warn(e.getMessage());
            }
            this.session = null;
        }
        // 原因为null为自主退出 无需立即终止session
        if (reason != null && subject != null) {
            subject.logout();
            this.subject = null;
        }
        this.online = false;
    }
}
