package com.socket.client.point;

import com.socket.client.config.SocketConfig;
import com.socket.client.manager.GroupManager;
import com.socket.client.manager.PermissionManager;
import com.socket.client.manager.UserManager;
import com.socket.client.model.UserPreview;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.client.util.Assert;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.support.SettingSupport;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.enums.MessageType;
import com.socket.webchat.model.enums.Setting;
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
    private static SettingSupport settingSupport;
    private static PermissionManager permissionManager;
    private static GroupManager groupManager;
    private static UserManager userManager;
    private WsUser self;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        // 登入
        this.self = userManager.join(session, config.getUserProperties());
        // 传递消息
        if (self != null) {
            Collection<UserPreview> userList = permissionManager.getUserPreviews(self);
            self.send(Callback.JOIN_INIT.get(), MessageType.INIT, userList);
            // 用户加入通知
            userManager.sendAll(Callback.USER_LOGIN.format(self), MessageType.JOIN, self);
            permissionManager.checkMute(self);
        }
    }

    @OnClose
    public void onClose() {
        // 退出通知
        if (self != null) {
            self.logout(null);
            userManager.remove(self.getUid());
            userManager.sendAll(Callback.USER_LOGOUT.format(self), MessageType.EXIT, self);
        }
    }

    @OnError
    public void onError(Throwable e) {
        log.error("", e);
    }

    @OnMessage
    public void onMessage(String message) {
        WsMsg wsmsg = self.decrypt(message);
        WsUser target = permissionManager.getTarget(wsmsg);
        // 系统消息
        if (wsmsg.isSysmsg()) {
            this.parseSysMsg(wsmsg, target);
            return;
        }
        // 用户消息
        this.parseUserMsg(wsmsg, target);
        userManager.cacheRecord(wsmsg, wsmsg.isReject() || wsmsg.isGroup() || target.chooseTarget(self));
    }

    public void parseUserMsg(WsMsg wsmsg, WsUser target) {
        // 禁言状态无法发送消息
        Assert.isFalse(permissionManager.isMute(self), Callback.SELF_MUTE);
        // 所有者全员禁言检查
        if (settingSupport.getSetting(Setting.ALL_MUTE) && !self.isOwner()) {
            self.reject(Callback.ALL_MUTE, wsmsg);
            return;
        }
        // 消息检查
        boolean sensitive = settingSupport.getSetting(Setting.SENSITIVE_WORDS);
        if (!permissionManager.verifyMessage(self, wsmsg, sensitive)) {
            return;
        }
        // 发言标记
        permissionManager.operateMark(self);
        // 群组消息
        if (wsmsg.isGroup()) {
            groupManager.sendGroup(wsmsg, self);
            self.send(wsmsg);
            return;
        }
        // 你屏蔽了目标
        Assert.isFalse(permissionManager.shield(self, target), Callback.TARGET_SHIELD);
        // 目标屏蔽了你
        if (permissionManager.shield(target, self)) {
            self.reject(Callback.SELF_SHIELD, wsmsg);
            return;
        }
        // AI消息智能回复
        if (settingSupport.getSetting(Setting.AI_MESSAGE)) {
            this.parseAiMessage(wsmsg);
        }
        // 发送至目标
        target.send(wsmsg);
        self.send(wsmsg);
    }

    /**
     * AI接管消息
     */
    private void parseAiMessage(WsMsg wsmsg) {
        boolean sysuid = Constants.SYSTEM_UID.equals(wsmsg.getTarget());
        boolean text = wsmsg.getType() == MessageType.TEXT;
        // 判断AI消息
        if (sysuid && text && !userManager.getUser(Constants.SYSTEM_UID).isOnline()) {
            userManager.sendAIMessage(self, wsmsg);
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
            case FOREVER:
                this.forever(target);
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
                permissionManager.pushNotice(wsmsg, self);
                break;
            default:
                self.send(Callback.INVALID_COMMAND.get(), MessageType.DANGER);
        }
    }

    /**
     * 屏蔽指定用户
     */
    private void shield(WsUser target) {
        boolean shield = permissionManager.shieldTarget(self, target);
        Callback tips = shield ? Callback.SHIELD_USER : Callback.CANCEL_SHIELD;
        self.send(tips.format(target), MessageType.SHIELD, target);
    }

    /**
     * 撤回消息
     */
    private void withdraw(WsUser target, WsMsg wsmsg) {
        ChatRecord record = userManager.withdrawMessage(wsmsg);
        // 转发系统消息
        if (record != null) {
            // 若此撤回的消息指向群组，则通知群组内所有人撤回此消息
            if (wsmsg.isGroup()) {
                groupManager.sendGroup(wsmsg, self);
            } else if (!permissionManager.shield(target, self)) {
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
        Assert.isFalse(permissionManager.shield(self, target), Callback.TARGET_SHIELD);
        Assert.isFalse(permissionManager.shield(target, self), Callback.TARGET_SHIELD);
        target.send(wsmsg);
    }

    /**
     * 用户列表选择变动,相关消息设为已读（群组消息默认已读）
     */
    private void choose(WsUser target, WsMsg wsmsg) {
        self.setChoose(wsmsg.getTarget());
        if (!wsmsg.isGroup() && userManager.getUnreadCount(self, target) > 0) {
            userManager.readAllMessage(self, target, false);
        }
    }

    /**
     * 禁言
     */
    private void mute(WsUser target, WsMsg wsmsg) {
        long time = permissionManager.addMute(wsmsg);
        // 禁言
        if (time > 0) {
            target.send(Callback.MUTE_LIMIT.format(time), MessageType.MUTE, time);
            userManager.sendAll(Callback.G_MUTE_LIMIT.format(target, time), MessageType.PRIMARY, target);
            return;
        }
        // 取消禁言
        target.send(Callback.C_MUTE_LIMIT.get(), MessageType.MUTE, time);
        userManager.sendAll(Callback.GC_MUTE_LIMIT.format(target, time), MessageType.PRIMARY, target);
    }

    /**
     * 限制登陆
     */
    private void lock(WsUser target, WsMsg wsmsg) {
        long time = permissionManager.addLock(wsmsg);
        // 向所有人发布消息
        if (time > 0) {
            // 大于0强制下线
            target.logout(Callback.LOGIN_LIMIT.format(time));
            userManager.sendAll(Callback.G_LOGIN_LIMIT.format(target, time), MessageType.DANGER, target);
            return;
        }
        userManager.sendAll(Callback.GC_LOGIN_LIMIT.format(target, time), MessageType.DANGER, target);
    }

    /**
     * 永久限制登录
     */
    private void forever(WsUser target) {
        target.logout(Callback.LIMIT_FOREVER.get());
        userManager.remove(target.getUid());
    }

    /**
     * 切换用户角色
     */
    private void switchRole(WsUser target) {
        permissionManager.updateRole(target, target.isAdmin() ? UserRole.USER : UserRole.ADMIN);
        // 通知目标
        String cb = (target.isAdmin() ? Callback.AUTH_ADMIN : Callback.AUTH_USER).get();
        target.send(cb, MessageType.ROLE, target);
        // 广播消息
        String gcb = (target.isAdmin() ? Callback.G_AUTH_ADMIN : Callback.G_AUTH_USER).format(target);
        userManager.sendAll(gcb, MessageType.ROLE, target);
    }

    /**
     * 设置头衔
     */
    private void setAlias(WsUser target, WsMsg wsmsg) {
        String alias = wsmsg.getContent();
        if (permissionManager.updateAlias(target, alias)) {
            target.send(alias, MessageType.ALIAS);
            userManager.sendAll(alias, MessageType.ALIAS, target);
        }
    }

    @Autowired
    private void setUserManager(UserManager manager) {
        WebSocketEndpoint.userManager = manager;
    }

    @Autowired
    private void setGroupManager(GroupManager manager) {
        WebSocketEndpoint.groupManager = manager;
    }

    @Autowired
    private void setGroupManager(PermissionManager manager) {
        WebSocketEndpoint.permissionManager = manager;
    }

    @Autowired
    public void setSettingSupport(SettingSupport support) {
        WebSocketEndpoint.settingSupport = support;
    }
}
