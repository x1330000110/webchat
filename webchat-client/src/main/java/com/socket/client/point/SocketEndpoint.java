package com.socket.client.point;

import com.socket.client.config.SocketConfig;
import com.socket.client.manager.PermissionManager;
import com.socket.client.manager.SocketGroupMap;
import com.socket.client.manager.SocketUserMap;
import com.socket.core.constant.Constants;
import com.socket.core.custom.support.SettingSupport;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.command.impl.CommandEnum;
import com.socket.core.model.enums.OnlineState;
import com.socket.core.model.enums.Setting;
import com.socket.core.model.ws.WsMsg;
import com.socket.core.model.ws.WsUser;
import com.socket.core.util.Enums;
import com.socket.secure.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.List;
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
            List<BaseUser> previews = permissionManager.getUserPreviews(user);
            user.send(CommandEnum.INIT.name(), CommandEnum.INIT, previews);
            // 向其他人发送加入通知
            userMap.sendAll(CommandEnum.JOIN, user);
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
            userMap.sendAll(CommandEnum.EXIT, user);
        });
    }

    @OnError
    public void onError(Throwable e) {
        if (e instanceof InvalidRequestException) {
            log.warn("安全验证失败：{}", e.getMessage());
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

    public void parseSysMsg(WsMsg wsmsg) {
        String target = wsmsg.getTarget();
        switch ((CommandEnum) wsmsg.getType()) {
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
     * WebRTC消息处理
     */
    private void forwardWebRTC(String tuid, WsMsg wsmsg) {
        WsUser target = userMap.get(tuid);
        // 目标用户空 忽略
        if (target == null) {
            return;
        }
        // 屏蔽检查
        if (permissionManager.shield(self, target)) {
            self.reject("您已屏蔽目标用户", wsmsg);
            return;
        }
        if (permissionManager.shield(target, self)) {
            self.send("对方屏蔽了你", CommandEnum.ERROR);
        }
        target.send(wsmsg);
    }

    /**
     * 在线状态变动事件
     *
     * @param state 状态
     */
    private void onlineChange(String state) {
        self.setOnline(Enums.of(OnlineState.class, state));
        userMap.sendAll(state, CommandEnum.CHANGE, self);
    }

    /**
     * 用户列表选择变动,相关消息设为已读（群组消息默认已读）
     */
    private void choose(String tuid, WsMsg wsmsg) {
        if (permissionManager.notHas(tuid)) {
            self.send("目标用户/群组不存在", CommandEnum.WARNING);
            return;
        }
        String suid = self.getGuid();
        self.setChoose(tuid);
        if (!wsmsg.isGroup() && userMap.getUnreadCount(suid, tuid) > 0) {
            userMap.readAllMessage(suid, tuid, false);
        }
    }

    public void parseUserMsg(WsMsg wsmsg) {
        // 禁言状态无法发送消息
        if (permissionManager.isMute(self)) {
            self.reject("您已被禁言，请稍后再试", wsmsg);
            return;
        }
        // 所有者全员禁言检查
        if (settingSupport.getSetting(Setting.ALL_MUTE) && !self.isOwner()) {
            self.reject("所有者开启了全员禁言", wsmsg);
            return;
        }
        // 检查目标是否存在
        String tuid = wsmsg.getTarget();
        if (permissionManager.notHas(tuid)) {
            self.reject("目标用户/群组不存在", wsmsg);
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
        // 你屏蔽了目标
        WsUser target = userMap.get(tuid);
        if (permissionManager.shield(self, target)) {
            self.reject("您已屏蔽目标用户", wsmsg);
            return;
        }
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
        boolean text = CommandEnum.TEXT == wsmsg.getType();
        // 判断AI消息
        if (sysuid && text && !userMap.get(Constants.SYSTEM_UID).isOnline()) {
            userMap.sendAIMessage(self, wsmsg);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        permissionManager = context.getBean(PermissionManager.class);
        settingSupport = context.getBean(SettingSupport.class);
        groupMap = context.getBean(SocketGroupMap.class);
        userMap = context.getBean(SocketUserMap.class);
    }
}