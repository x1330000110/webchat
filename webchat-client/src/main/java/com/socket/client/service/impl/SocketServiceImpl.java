package com.socket.client.service.impl;

import com.socket.client.config.SocketConfig;
import com.socket.client.manager.SocketManager;
import com.socket.client.model.UserPreview;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.CallbackTips;
import com.socket.client.model.enums.Remote;
import com.socket.client.service.SocketService;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.enums.MessageType;
import com.socket.webchat.model.enums.UserRole;
import com.socket.webchat.request.XiaoBingAPIRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.Map;

/**
 * websocket服务处理
 */
@Slf4j
@Service
@ServerEndpoint(value = "/user/room", configurator = SocketConfig.class)
public class SocketServiceImpl implements SocketService {
    private static SocketManager socketManager;
    private static XiaoBingAPIRequest robot;
    private WsUser self;

    @Autowired
    private void setAutowired(SocketManager socketManager, XiaoBingAPIRequest robot) {
        SocketServiceImpl.socketManager = socketManager;
        SocketServiceImpl.robot = robot;
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        Map<String, Object> properties = config.getUserProperties();
        // 收集数据
        Subject subject = (Subject) properties.get(Constants.SUBJECT);
        HttpSession hsession = (HttpSession) properties.get(Constants.HTTP_SESSION);
        this.self = socketManager.join(subject, session, hsession);
        // 同步登录平台
        self.setPlatform((String) properties.get(Constants.PLATFORM));
        if (self != null) {
            Collection<UserPreview> userList = socketManager.getUserList(self);
            WsMsg.buildsys(CallbackTips.JOIN_INIT.of(), MessageType.INIT, userList).send(self, Remote.ASYNC);
            socketManager.sendAll(CallbackTips.USER_LOGIN.of(self), MessageType.JOIN, self);
            socketManager.checkMute(self);
        }
    }

    @OnClose
    public void onClose() {
        // 解决ws共享问题
        if (self != null) {
            socketManager.sendAll(CallbackTips.USER_LOGOUT.of(self), MessageType.EXIT, self);
            socketManager.remove(self);
        }
    }

    @OnError
    public void onError(Throwable e) {
        log.info("SOCKET ERROR:", e);
    }

    @OnMessage
    public String onMessage(String json) {
        WsMsg wsmsg = self.decrypt(json);
        wsmsg.setUid(self.getUid());
        WsUser target = socketManager.getUser(wsmsg.getTarget());
        // 检查目标有效性
        if (!wsmsg.isGroup() && target == null) {
            return self.encrypt(WsMsg.buildsys(CallbackTips.USER_NOT_FOUND.of(), MessageType.DANGER));
        }
        return self.encrypt(wsmsg.isSysmsg() ? parseSysMsg(wsmsg, target) : parseUserMsg(wsmsg, target));
    }

    @Override
    public WsMsg parseUserMsg(WsMsg wsmsg, WsUser target) {
        // 禁言状态无法发送消息
        if (socketManager.isMute(self.getUid())) {
            return WsMsg.buildsys(CallbackTips.SELF_IS_MUTE.of(), MessageType.DANGER);
        }
        // HTML脚本过滤
        wsmsg.checkMessage();
        // AI消息智能回复
        this.parseAiMessage(wsmsg);
        // 发言标记
        socketManager.operateMark(self);
        try {
            // 群组消息
            if (wsmsg.isGroup()) {
                socketManager.sendAll(wsmsg, self);
                return wsmsg;
            }
            // 检查屏蔽
            if (socketManager.shield(self, target)) {
                return WsMsg.buildsys(CallbackTips.TARGET_SHIELD.of(), MessageType.INFO);
            }
            if (socketManager.shield(target, self)) {
                wsmsg.setReject(true);
                wsmsg.send(self, Remote.SYNC);
                return WsMsg.buildsys(CallbackTips.SELF_SHIELD.of(), MessageType.WARNING);
            }
            // 发送至目标
            wsmsg.send(target, Remote.ASYNC);
        } finally {
            // 保存消息（已读条件：消息未能送达 || 消息来自群组 || 目标会话正在选择你）
            socketManager.cacheRecord(wsmsg, wsmsg.isReject() || wsmsg.isGroup() || target.chooseTarget(self));
        }
        return wsmsg;
    }

    /**
     * AI接管消息
     */
    private void parseAiMessage(WsMsg wsmsg) {
        boolean sysuid = Constants.SYSTEM_UID.equals(wsmsg.getTarget());
        boolean text = wsmsg.getType() == MessageType.TEXT;
        // 判断AI消息
        if (sysuid && text && socketManager.getOnline(Constants.SYSTEM_UID) == null) {
            robot.dialogue(wsmsg.getContent()).addCallback(result -> {
                if (result != null) {
                    // AI消息
                    WsMsg aimsg = WsMsg.buildmsg(Constants.SYSTEM_UID, wsmsg.getUid(), result, MessageType.TEXT);
                    aimsg.send(self, Remote.ASYNC);
                    socketManager.cacheRecord(aimsg, true);
                }
            }, exception -> log.warn(exception.getMessage()));
        }
    }

    @Override
    public WsMsg parseSysMsg(WsMsg wsmsg, WsUser target) {
        switch (wsmsg.getType()) {
            case SHIELD:
                return this.shield(target);
            case REMOVE:
                return this.remove(target, wsmsg);
            case CHOOSE:
                this.choose(target, wsmsg);
                return null;
            case ANSWER:
            case OFFER:
            case CANDIDATE:
            case LEAVE:
            case VIDEO:
            case AUDIO:
                return this.forwardWebRTC(target, wsmsg);
            default:
                return this.parseAdminSysMsg(wsmsg, target);
        }
    }

    @Override
    public WsMsg parseAdminSysMsg(WsMsg wsmsg, WsUser target) {
        // 发起者不是管理员 || 发起者是管理员目标是所有者
        if (!self.isAdmin() || !self.isOwner() && target.isAdmin()) {
            return WsMsg.buildsys(CallbackTips.REJECT_EXECUTE.of(), MessageType.DANGER);
        }
        switch (wsmsg.getType()) {
            case MUTE:
                this.mute(target, wsmsg);
                break;
            case LOCK:
                this.lock(target, wsmsg);
                break;
            default:
                return this.parseOwnerSysMsg(wsmsg, target);
        }
        return null;
    }

    @Override
    public WsMsg parseOwnerSysMsg(WsMsg wsmsg, WsUser target) {
        if (!self.isOwner()) {
            return WsMsg.buildsys(CallbackTips.REJECT_EXECUTE.of(), MessageType.WARNING);
        }
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
                return WsMsg.buildsys(CallbackTips.COMMAND_INCORRECT.of(), MessageType.DANGER);
        }
        return null;
    }

    /**
     * 屏蔽指定用户
     */
    private WsMsg shield(WsUser target) {
        boolean shield = socketManager.shieldTarget(self, target);
        CallbackTips tips = shield ? CallbackTips.SHIELD_USER.of(target) : CallbackTips.CANCEL_SHIELD.of(target);
        WsMsg.buildsys(tips, MessageType.SHIELD, target).send(self, Remote.ASYNC);
        return null;
    }

    /**
     * 撤回消息
     */
    private WsMsg remove(WsUser target, WsMsg wsmsg) {
        ChatRecord record = socketManager.removeMessage(wsmsg);
        if (record != null) {
            // 若此撤回的消息指向群组，则通知所有人撤回此消息
            if (wsmsg.isGroup()) {
                socketManager.sendAll(wsmsg, self);
            } else if (!socketManager.shield(target, self)) {
                // 反之 仅通知目标撤回此消息（若目标已将此用户屏蔽，则忽略此撤回消息）
                wsmsg.send(target, Remote.ASYNC);
            }
            // 若这是一条未能送达是消息 则不提示任何回调
            if (record.isReject()) {
                return null;
            }
            return wsmsg;
        }
        CallbackTips tips = CallbackTips.WITHDRAW_TIMEDOUT.of(Constants.WITHDRAW_MESSAGE_TIME);
        return WsMsg.buildsys(tips, MessageType.WARNING);
    }

    /**
     * WebRTC消息处理
     */
    @SneakyThrows
    private WsMsg forwardWebRTC(WsUser target, WsMsg wsmsg) {
        // 你是否屏蔽了目标
        if (socketManager.shield(self, target)) {
            return WsMsg.buildsys(CallbackTips.TARGET_SHIELD.of(), MessageType.INFO);
        }
        // 目标是否屏蔽你
        if (socketManager.shield(target, self)) {
            return WsMsg.buildsys(CallbackTips.SELF_SHIELD.of(), MessageType.VIDEO);
        }
        wsmsg.send(target, Remote.SYNC);
        return null;
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
            WsMsg.buildsys(CallbackTips.MUTE_LIMIT.of(time), MessageType.MUTE, time).send(target, Remote.ASYNC);
            socketManager.sendAll(CallbackTips.GLOBAL_MUTE_LIMIT.of(target, time), MessageType.PRIMARY, target);
            return;
        }
        // 取消禁言
        WsMsg.buildsys(CallbackTips.CANCEL_MUTE_LIMIT.of(), MessageType.MUTE, time).send(target, Remote.ASYNC);
        socketManager.sendAll(CallbackTips.GLOBAL_CANCEL_MUTE_LIMIT.of(target, time), MessageType.PRIMARY, target);
    }

    /**
     * 限制登陆
     */
    private void lock(WsUser target, WsMsg wsmsg) {
        // 永久限制登录处理
        if (Constants.FOREVER.equalsIgnoreCase(wsmsg.getContent())) {
            target.logout(CallbackTips.LOGIN_LIMIT_FOREVER.of());
            return;
        }
        long time = socketManager.addLock(wsmsg);
        // 向所有人发布消息
        if (time > 0) {
            // 大于0强制下线
            target.logout(CallbackTips.LOGIN_LIMIT.of(time));
            socketManager.sendAll(CallbackTips.GLOBAL_LOGIN_LIMIT.of(target, time), MessageType.DANGER, target);
            return;
        }
        socketManager.sendAll(CallbackTips.GLOBAL_CANCEL_LOGIN_LIMIT.of(target, time), MessageType.DANGER, target);
    }

    /**
     * 切换用户角色
     */
    private void switchRole(WsUser target) {
        socketManager.updateRole(target, target.isAdmin() ? UserRole.USER : UserRole.ADMIN);
        // 通知目标
        CallbackTips selfTips = (target.isAdmin() ? CallbackTips.ADMIN : CallbackTips.USERS).of();
        WsMsg.buildsys(selfTips, MessageType.ROLE, target).send(target, Remote.ASYNC);
        // 广播消息
        CallbackTips globalTips = (target.isAdmin() ? CallbackTips.GLOBAL_ADMIN : CallbackTips.GLOBA_USERS).of(target);
        socketManager.sendAll(globalTips, MessageType.ROLE, target);
    }

    /**
     * 设置头衔
     */
    private void setAlias(WsUser target, WsMsg wsmsg) {
        String alias = wsmsg.getContent();
        if (socketManager.updateAlias(target, alias)) {
            wsmsg.send(self, Remote.ASYNC);
            socketManager.sendAll(wsmsg, self);
        }
    }
}
