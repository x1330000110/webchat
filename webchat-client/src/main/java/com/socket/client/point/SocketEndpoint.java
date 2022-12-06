package com.socket.client.point;

import com.socket.client.config.SocketConfig;
import com.socket.client.exception.SocketException;
import com.socket.client.manager.PermissionManager;
import com.socket.client.manager.SocketGroupMap;
import com.socket.client.manager.SocketUserMap;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.OnlineState;
import com.socket.client.util.Assert;
import com.socket.secure.exception.InvalidRequestException;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.support.SettingSupport;
import com.socket.webchat.model.BaseUser;
import com.socket.webchat.model.command.impl.MessageEnum;
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
    private static SocketGroupMap groupMap;
    private static SocketUserMap userMap;
    private WsUser self;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        // 加入聊天室
        Optional.ofNullable(userMap.join(session, config.getUserProperties())).ifPresent(user -> {
            // 推送所有用户数据
            Collection<BaseUser> userList = permissionManager.getUserPreviews(user);
            user.send("Initialize user", MessageEnum.INIT, userList);
            // 向其他人发送加入通知
            userMap.sendAll(MessageEnum.JOIN, user);
            // 检查禁言
            permissionManager.checkMute(user);
            this.self = user;
        });
    }

    @OnClose
    public void onClose() {
        Optional.ofNullable(self).ifPresent(user -> {
            userMap.exit(user, null);
            // 退出通知
            userMap.sendAll(MessageEnum.EXIT, user);
        });
    }

    @OnError
    public void onError(Throwable e) {
        if (e instanceof InvalidRequestException) {
            log.warn("安全验证失败：{}", e.getMessage());
            return;
        }
        if (e instanceof SocketException) {
            log.warn("非法操作拦截：{}", e.getMessage());
            return;
        }
        e.printStackTrace();
    }

    @OnMessage
    public void onMessage(String message) {
        WsMsg wsmsg = self.decrypt(message);
        if (wsmsg.isSysmsg()) {
            // 系统消息
            this.parseSysMsg(wsmsg);
        } else {
            // 用户消息
            this.parseUserMsg(wsmsg);
        }
    }

    public void parseUserMsg(WsMsg wsmsg) {
        // 禁言状态无法发送消息
        Assert.isFalse(permissionManager.isMute(self), "您已被禁言，请稍后再试");
        // 所有者全员禁言检查
        if (settingSupport.getSetting(Setting.ALL_MUTE) && !self.isOwner()) {
            self.reject("所有者开启了全员禁言", wsmsg);
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
            groupMap.sendGroup(wsmsg);
            userMap.cacheRecord(wsmsg, true);
            return;
        }
        WsUser target = userMap.get(wsmsg.getTarget());
        if (target == null) {
            self.reject("目标不存在或已被注销", wsmsg);
            return;
        }
        // 你屏蔽了目标
        Assert.isFalse(permissionManager.shield(self, target), "意外的错误，你在屏蔽对方时发送了消息");
        try {
            // 目标屏蔽了你
            if (permissionManager.shield(target, self)) {
                self.reject("消息未送达，您已被目标用户屏蔽", wsmsg);
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
            userMap.cacheRecord(wsmsg, wsmsg.isReject() || wsmsg.isGroup() || target.chooseTarget(self));
        }
    }

    /**
     * AI接管消息
     */
    private void parseAiMessage(WsMsg wsmsg) {
        boolean sysuid = Constants.SYSTEM_UID.equals(wsmsg.getTarget());
        boolean text = Objects.equals(wsmsg.getType(), MessageEnum.TEXT.toString());
        // 判断AI消息
        if (sysuid && text && !userMap.get(Constants.SYSTEM_UID).isOnline()) {
            userMap.sendAIMessage(self, wsmsg);
        }
    }

    public void parseSysMsg(WsMsg wsmsg) {
        MessageEnum command = MessageEnum.valueOf(wsmsg.getType().toUpperCase());
        String target = wsmsg.getTarget();
        switch (command) {
            case CHANGE:
                this.onlineChange(wsmsg.getContent());
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
     * 在线状态变动事件
     *
     * @param state 状态
     */
    private void onlineChange(String state) {
        self.setOnline(OnlineState.of(state));
        userMap.sendAll(state, MessageEnum.CHANGE, self);
    }

    /**
     * 用户列表选择变动,相关消息设为已读（群组消息默认已读）
     */
    private void choose(String tuid, WsMsg wsmsg) {
        WsUser target = permissionManager.getTarget(wsmsg);
        if (target == null) {
            self.send("目标不存在或已被注销", MessageEnum.WARNING);
            return;
        }
        self.setChoose(tuid);
        if (!wsmsg.isGroup() && userMap.getUnreadCount(self, target) > 0) {
            userMap.readAllMessage(self, target, false);
        }
    }

    /**
     * WebRTC消息处理
     */
    private void forwardWebRTC(String tuid, WsMsg wsmsg) {
        WsUser target = userMap.get(tuid);
        // 目标用户空 忽略
        if (target == null) {
            return;
        }
        // 屏蔽检查
        Assert.isFalse(permissionManager.shield(self, target), "屏蔽目标时无法发起通话请求");
        if (permissionManager.shield(target, self)) {
            self.send("对方屏蔽了你", MessageEnum.ERROR);
        }
        target.send(wsmsg);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        permissionManager = context.getBean(PermissionManager.class);
        settingSupport = context.getBean(SettingSupport.class);
        groupMap = context.getBean(SocketGroupMap.class);
        userMap = context.getBean(SocketUserMap.class);
    }
}
