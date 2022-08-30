package com.socket.client.model;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.json.JSONUtil;
import com.socket.client.model.enums.Callback;
import com.socket.secure.util.AES;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.enums.UserRole;
import lombok.Getter;
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
public class WsUser extends SysUser {
    /**
     * WebSocket Session
     */
    @Getter
    private Session session;
    /**
     * Http Session
     */
    private HttpSession http_session;
    /**
     * Shiro Subject
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
     * 构建ws用户信息
     *
     * @param sysUser 用户信息
     */
    public WsUser(SysUser sysUser) {
        BeanUtil.copyProperties(sysUser, this);
    }

    /**
     * 构建ws用户信息
     *
     * @param subject      shiro subject
     * @param session      Websocket session
     * @param http_session Http Session
     */
    public WsUser(Subject subject, Session session, HttpSession http_session) {
        this((SysUser) subject.getPrincipal());
        this.subject = subject;
        this.session = session;
        this.http_session = http_session;
        this.online = true;
    }

    /**
     * 退出聊天室
     *
     * @param reason 原因（强制退出时填写）
     */
    public void logout(Callback reason) {
        // 始终清除ws会话
        if (session != null) {
            try {
                String str = Opt.ofNullable(reason).map(Callback::getReason).get();
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
     * 加密消息
     */
    public String encrypt(WsMsg wsmsg) {
        if (wsmsg != null) {
            return AES.encrypt(JSONUtil.toJsonStr(wsmsg), http_session);
        }
        return null;
    }

    /**
     * 解密消息
     */
    public WsMsg decrypt(String json) {
        return JSONUtil.toBean(AES.decrypt(json, http_session), WsMsg.class);
    }
}
