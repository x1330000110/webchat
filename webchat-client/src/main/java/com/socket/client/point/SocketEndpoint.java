package com.socket.client.point;

import com.socket.client.config.SocketConfig;
import com.socket.client.manager.GroupManager;
import com.socket.client.manager.PermissionManager;
import com.socket.client.manager.UserManager;
import com.socket.client.model.UserPreview;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.client.model.enums.OnlineState;
import com.socket.client.util.Assert;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.support.SettingSupport;
import com.socket.webchat.model.enums.MessageType;
import com.socket.webchat.model.enums.Setting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Ws聊天室用户与消息处理
 */
@Slf4j
@Service
@ServerEndpoint(value = "/user/room", configurator = SocketConfig.class)
public class SocketEndpoint implements ApplicationContextAware {
    private static PermissionManager permissionManager;
    private static SettingSupport settingSupport;
    private static GroupManager groupManager;
    private static UserManager userManager;
    private WsUser self;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        // 加入聊天室
        Optional.ofNullable(userManager.join(session, config.getUserProperties())).ifPresent(user -> {
            // 推送所有用户数据
            Collection<UserPreview> userList = permissionManager.getUserPreviews(user);
            user.send(Callback.JOIN_INIT.get(), MessageType.INIT, userList);
            // 向其他人发送加入通知
            userManager.sendAll(MessageType.JOIN, user);
            // 检查禁言
            permissionManager.checkMute(user);
            this.self = user;
        });
    }

    @OnClose
    public void onClose() {
        Optional.ofNullable(self).ifPresent(user -> {
            userManager.exit(user, null);
            // 退出通知
            userManager.sendAll(MessageType.EXIT, user);
        });
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
            groupManager.sendGroup(wsmsg);
            userManager.cacheRecord(wsmsg, true);
            return;
        }
        // 你屏蔽了目标
        Assert.isFalse(permissionManager.shield(self, target), Callback.TARGET_SHIELD);
        try {
            // 目标屏蔽了你
            if (permissionManager.shield(target, self)) {
                self.reject(Callback.SELF_SHIELD, wsmsg);
                return;
            }
            // 发送至目标
            target.send(wsmsg);
            self.send(wsmsg);
            // AI消息智能回复
            if (settingSupport.getSetting(Setting.AI_MESSAGE)) {
                this.parseAiMessage(wsmsg);
            }
        } finally {
            // 已读条件：消息未送达，目标是群组，目标正在选择你
            userManager.cacheRecord(wsmsg, wsmsg.isReject() || wsmsg.isGroup() || target.chooseTarget(self));
        }
    }

    /**
     * AI接管消息
     */
    private void parseAiMessage(WsMsg wsmsg) {
        boolean sysuid = Constants.SYSTEM_UID.equals(wsmsg.getTarget());
        boolean text = Objects.equals(wsmsg.getType(), MessageType.TEXT.toString());
        // 判断AI消息
        if (sysuid && text && !userManager.getUser(Constants.SYSTEM_UID).isOnline()) {
            userManager.sendAIMessage(self, wsmsg);
        }
    }

    public void parseSysMsg(WsMsg wsmsg, WsUser target) {
        String type = wsmsg.getType().toUpperCase();
        switch (MessageType.valueOf(type)) {
            case CHANGE:
                String content = wsmsg.getContent();
                self.setOnline(OnlineState.of(content));
                userManager.sendAll(content, MessageType.CHANGE, self);
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
                // ignore
        }
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

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        permissionManager = context.getBean(PermissionManager.class);
        settingSupport = context.getBean(SettingSupport.class);
        groupManager = context.getBean(GroupManager.class);
        userManager = context.getBean(UserManager.class);
    }
}
