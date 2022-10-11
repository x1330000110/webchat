package com.socket.client.support;

import com.socket.client.config.SocketConfig;
import com.socket.client.manager.SocketManager;
import com.socket.client.model.UserPreview;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.client.util.Assert;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.enums.MessageType;
import com.socket.webchat.model.enums.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Collection;

/**
 * Ws聊天室用户与消息处理
 */
@Slf4j
@Service
@ServerEndpoint(value = "/user/room", configurator = SocketConfig.class)
public class WebSocketEndpoint {
    private static SocketManager socketManager;
    private WsUser self;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        // 登入
        this.self = socketManager.join(session, config.getUserProperties());
        // 传递消息
        if (self != null) {
            Collection<UserPreview> userList = socketManager.getPreviews(self);
            self.send(Callback.JOIN_INIT.get(), MessageType.INIT, userList);
            // 用户加入通知
            socketManager.sendAll(Callback.USER_LOGIN.format(self), MessageType.JOIN, self);
            socketManager.checkMute(self);
        }
    }

    @OnClose
    public void onClose() {
        // 退出通知
        if (self != null) {
            socketManager.sendAll(Callback.USER_LOGOUT.format(self), MessageType.EXIT, self);
            socketManager.remove(self, false);
        }
    }

    @OnError
    public void onError(Throwable e) {
        log.error("", e);
    }

    @OnMessage
    public void onMessage(String message) {
        WsMsg wsmsg = self.decrypt(message);
        WsUser target = socketManager.getTarget(wsmsg);
        // 系统消息
        if (wsmsg.isSysmsg()) {
            this.parseSysMsg(wsmsg, target);
            return;
        }
        // 用户消息
        this.parseUserMsg(wsmsg, target);
        socketManager.cacheRecord(wsmsg, wsmsg.isReject() || wsmsg.isGroup() || target.chooseTarget(self));
    }

    public void parseUserMsg(WsMsg wsmsg, WsUser target) {
        // 禁言状态无法发送消息
        Assert.isFalse(socketManager.isMute(self), Callback.SELF_MUTE);
        // 消息检查
        socketManager.checkMessage(self, wsmsg);
        if (wsmsg.isReject()) {
            self.send(Callback.SENSITIVE_KEYWORDS.get(), MessageType.WARNING);
            return;
        }
        // AI消息智能回复
        this.parseAiMessage(wsmsg);
        // 发言标记
        socketManager.operateMark(self);
        // 群组消息
        if (wsmsg.isGroup()) {
            socketManager.sendGroup(wsmsg, self);
            self.send(wsmsg.accept());
            return;
        }
        // 你屏蔽了目标
        Assert.isFalse(socketManager.shield(self, target), Callback.TARGET_SHIELD);
        // 目标屏蔽了你
        if (socketManager.shield(target, self)) {
            self.send(wsmsg.reject());
            self.send(Callback.SELF_SHIELD.get(), MessageType.WARNING);
            return;
        }
        // 发送至目标
        target.send(wsmsg);
        self.send(wsmsg.accept());
    }

    /**
     * AI接管消息
     */
    private void parseAiMessage(WsMsg wsmsg) {
        boolean sysuid = Constants.SYSTEM_UID.equals(wsmsg.getTarget());
        boolean text = wsmsg.getType() == MessageType.TEXT;
        // 判断AI消息
        if (sysuid && text && !socketManager.getUser(Constants.SYSTEM_UID).isOnline()) {
            socketManager.sendAIMessage(self, wsmsg);
        }
    }

    public void parseSysMsg(WsMsg wsmsg, WsUser target) {
        switch (wsmsg.getType()) {
            case SHIELD:
                this.shield(target);
                break;
            case WITHDRAW:
                this.withdraw(target, wsmsg);
                break;
            case CHOOSE:
                this.choose(target, wsmsg);
                break;
            case ANSWER:
            case OFFER:
            case CANDIDATE:
            case LEAVE:
            case VIDEO:
            case AUDIO:
                this.forwardWebRTC(target, wsmsg);
                break;
            default:
                this.parseAdminSysMsg(wsmsg, target);
        }
    }

    public void parseAdminSysMsg(WsMsg wsmsg, WsUser target) {
        // 管理员权限检查
        Assert.isAdmin(self, target, Callback.REJECT_EXECUTE);
        switch (wsmsg.getType()) {
            case MUTE:
                this.mute(target, wsmsg);
                break;
            case LOCK:
                this.lock(target, wsmsg);
                break;
            default:
                this.parseOwnerSysMsg(wsmsg, target);
        }
    }

    public void parseOwnerSysMsg(WsMsg wsmsg, WsUser target) {
        // 所有者权限检查
        Assert.isOwner(self, Callback.REJECT_EXECUTE);
        switch (wsmsg.getType()) {
            case ROLE:
                this.switchRole(target);
                break;
            case ALIAS:
                this.setAlias(target, wsmsg);
                break;
            case ANNOUNCE:
                socketManager.pushNotice(wsmsg, self);
                break;
            default:
                self.send(Callback.INVALID_COMMAND.get(), MessageType.DANGER);
        }
    }

    /**
     * 屏蔽指定用户
     */
    private void shield(WsUser target) {
        boolean shield = socketManager.shieldTarget(self, target);
        Callback tips = shield ? Callback.SHIELD_USER : Callback.CANCEL_SHIELD;
        self.send(tips.format(target), MessageType.SHIELD, target);
    }

    /**
     * 撤回消息
     */
    private void withdraw(WsUser target, WsMsg wsmsg) {
        ChatRecord record = socketManager.removeMessage(wsmsg);
        // 转发系统消息
        if (record != null) {
            // 若此撤回的消息指向群组，则通知群组内所有人撤回此消息
            if (wsmsg.isGroup()) {
                socketManager.sendGroup(wsmsg, self);
            } else if (!socketManager.shield(target, self)) {
                // 仅通知目标撤回此消息（若目标已将此用户屏蔽，则忽略此撤回消息）
                target.send(wsmsg);
            }
            // 若这是一条未能送达是消息 则不提示任何回调
            if (!record.isReject()) {
                self.send(wsmsg);
            }
            return;
        }
        self.send(Callback.WITHDRAW_FAILURE.get(), MessageType.WARNING, Constants.WITHDRAW_TIME);
    }

    /**
     * WebRTC消息处理
     */
    private void forwardWebRTC(WsUser target, WsMsg wsmsg) {
        // 屏蔽检查
        Assert.isFalse(socketManager.shield(self, target), Callback.TARGET_SHIELD);
        Assert.isFalse(socketManager.shield(target, self), Callback.TARGET_SHIELD);
        target.send(wsmsg);
    }

    /**
     * 用户列表选择变动,相关消息设为已读（群组消息默认已读）
     */
    private void choose(WsUser target, WsMsg wsmsg) {
        self.setChoose(wsmsg.getTarget());
        if (!wsmsg.isGroup() && socketManager.getUnreadCount(self, target.getUid()) > 0) {
            socketManager.readAllMessage(self, target, false);
        }
    }

    /**
     * 禁言
     */
    private void mute(WsUser target, WsMsg wsmsg) {
        long time = socketManager.addMute(wsmsg);
        // 禁言
        if (time > 0) {
            target.send(Callback.MUTE_LIMIT.format(time), MessageType.MUTE, time);
            socketManager.sendAll(Callback.G_MUTE_LIMIT.format(target, time), MessageType.PRIMARY, target);
            return;
        }
        // 取消禁言
        target.send(Callback.C_MUTE_LIMIT.get(), MessageType.MUTE, time);
        socketManager.sendAll(Callback.GC_MUTE_LIMIT.format(target, time), MessageType.PRIMARY, target);
    }

    /**
     * 限制登陆
     */
    private void lock(WsUser target, WsMsg wsmsg) {
        // 永久限制登录处理
        if (Constants.FOREVER.equalsIgnoreCase(wsmsg.getContent())) {
            target.logout(Callback.LIMIT_FOREVER.get());
            socketManager.remove(target, true);
            return;
        }
        long time = socketManager.addLock(wsmsg);
        // 向所有人发布消息
        if (time > 0) {
            // 大于0强制下线
            target.logout(Callback.LOGIN_LIMIT.format(time));
            socketManager.sendAll(Callback.G_LOGIN_LIMIT.format(target, time), MessageType.DANGER, target);
            return;
        }
        socketManager.sendAll(Callback.GC_LOGIN_LIMIT.format(target, time), MessageType.DANGER, target);
    }

    /**
     * 切换用户角色
     */
    private void switchRole(WsUser target) {
        socketManager.updateRole(target, target.isAdmin() ? UserRole.USER : UserRole.ADMIN);
        // 通知目标
        String cb = (target.isAdmin() ? Callback.AUTH_ADMIN : Callback.AUTH_USER).get();
        target.send(cb, MessageType.ROLE, target);
        // 广播消息
        String gcb = (target.isAdmin() ? Callback.G_AUTH_ADMIN : Callback.G_AUTH_USER).format(target);
        socketManager.sendAll(gcb, MessageType.ROLE, target);
    }

    /**
     * 设置头衔
     */
    private void setAlias(WsUser target, WsMsg wsmsg) {
        String alias = wsmsg.getContent();
        if (socketManager.updateAlias(target, alias)) {
            self.send(wsmsg);
            socketManager.sendGroup(wsmsg, self);
        }
    }

    @Autowired
    private void setSocketManager(SocketManager manager) {
        WebSocketEndpoint.socketManager = manager;
    }
}
