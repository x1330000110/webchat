package com.socket.client.point;

import com.socket.client.manager.GroupManager;
import com.socket.client.manager.PermissionManager;
import com.socket.client.manager.UserManager;
import com.socket.client.model.SocketMessage;
import com.socket.client.model.SocketUser;
import com.socket.core.custom.SettingSupport;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.command.impl.CommandEnum;
import com.socket.core.model.enums.OnlineState;
import com.socket.core.model.enums.Setting;
import com.socket.core.util.Enums;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.yeauty.annotation.*;
import org.yeauty.pojo.Session;

import java.util.List;

/**
 * Socket端点处理类
 */
@Slf4j
@Component
@ServerEndpoint(value = "/ws/room", port = "${server.port}")
public class SocketEndpoint implements ApplicationContextAware {
    private static final String TOKEN = "Sec-WebSocket-Protocol";
    private static PermissionManager permissionManager;
    private static SettingSupport settingSupport;
    private static GroupManager groupManager;
    private static UserManager userManager;
    private SocketUser self;

    @BeforeHandshake
    public void handshake(Session session, HttpHeaders headers) {
        String token = headers.get(TOKEN);
        // token认证
        this.self = userManager.join(session, token);
        if (self != null) {
            session.setSubprotocols(token);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        if (self != null) {
            log.info("===> 用户加入聊天室：{}", self.getGuid());
            // 推送所有用户数据
            List<BaseUser> previews = permissionManager.getUserPreviews(self);
            self.send(CommandEnum.INIT.name(), CommandEnum.INIT, previews);
            // 向其他人发送加入通知
            userManager.sendAll(CommandEnum.JOIN, self);
            // 检查禁言
            permissionManager.checkMute(self);
        }
    }

    @OnClose
    public void onClose() {
        if (self != null) {
            log.info("<=== 用户退出聊天室：{}", self.getGuid());
            // 退出通知
            userManager.exit(self, null);
            userManager.sendAll(CommandEnum.EXIT, self);
        }
    }

    @OnError
    public void onError(Throwable e) {
        e.printStackTrace();
    }

    @OnMessage
    public void onMessage(String str) {
        SocketMessage message = self.decrypt(str);
        if (message.isSysmsg()) {
            this.parseSysMsg(message);
        } else {
            this.parseUserMsg(message);
        }
    }

    public void parseSysMsg(SocketMessage message) {
        String target = message.getTarget();
        switch ((CommandEnum) message.getType()) {
            case CHANGE:
                this.onlineChange(message.getContent());
                break;
            case CHOOSE:
                this.choose(target, message);
                break;
            case ANSWER:
            case OFFER:
            case CANDIDATE:
            case LEAVE:
            case VIDEO:
            case AUDIO:
                this.forwardWebRTC(target, message);
                break;
            default:
                // ignore
        }
    }

    public void parseUserMsg(SocketMessage message) {
        // 禁言状态无法发送消息
        if (permissionManager.isMute(self)) {
            self.reject("您已被禁言，请稍后再试", message);
            return;
        }
        // 所有者全员禁言检查
        if (settingSupport.getSetting(Setting.ALL_MUTE) && !self.isOwner()) {
            self.reject("所有者开启了全员禁言", message);
            return;
        }
        // 检查目标是否存在
        String tuid = message.getTarget();
        if (permissionManager.notHas(tuid)) {
            self.reject("目标用户/群组不存在", message);
            return;
        }
        // 消息检查
        boolean sensitive = settingSupport.getSetting(Setting.SENSITIVE_WORDS);
        if (!permissionManager.verifyMessage(self, message, sensitive)) {
            return;
        }
        // 发言标记
        permissionManager.operateMark(self);
        // 群组消息
        if (message.isGroup()) {
            groupManager.sendGroup(message);
            userManager.cacheRecord(message, true);
            return;
        }
        // 你屏蔽了目标
        SocketUser target = userManager.get(tuid);
        if (permissionManager.shield(self, target)) {
            self.reject("您已屏蔽目标用户", message);
            return;
        }
        try {
            // 目标屏蔽了你
            if (permissionManager.shield(target, self)) {
                self.reject("消息未送达，您已被目标用户屏蔽", message);
                return;
            }
            // 发送至目标
            target.send(message);
            self.send(message);
            // AI消息智能回复
            if (settingSupport.getSetting(Setting.AI_MESSAGE)) {
                userManager.sendAIMessage(self, message);
            }
        } finally {
            // 已读条件：消息未送达，目标是群组，目标正在选择你
            userManager.cacheRecord(message, message.isReject() || message.isGroup() || target.chooseTarget(self));
        }
    }

    /**
     * 在线状态变动事件
     *
     * @param state 状态
     */
    private void onlineChange(String state) {
        self.setOnline(Enums.of(OnlineState.class, state));
        userManager.sendAll(state, CommandEnum.CHANGE, self);
    }

    /**
     * 用户列表选择变动,相关消息设为已读（群组消息默认已读）
     */
    private void choose(String tuid, SocketMessage message) {
        if (permissionManager.notHas(tuid)) {
            self.send("目标用户/群组不存在", CommandEnum.WARNING);
            return;
        }
        String suid = self.getGuid();
        self.setChoose(tuid);
        if (!message.isGroup() && userManager.getUnreadCount(suid, tuid) > 0) {
            userManager.readAllMessage(suid, tuid, false);
        }
    }

    /**
     * WebRTC消息处理
     */
    private void forwardWebRTC(String tuid, SocketMessage message) {
        SocketUser target = userManager.get(tuid);
        // 目标用户空 忽略
        if (target == null) {
            return;
        }
        // 屏蔽检查
        if (permissionManager.shield(self, target)) {
            self.reject("您已屏蔽目标用户", message);
            return;
        }
        if (permissionManager.shield(target, self)) {
            self.send("对方屏蔽了你", CommandEnum.ERROR);
        }
        target.send(message);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        permissionManager = context.getBean(PermissionManager.class);
        settingSupport = context.getBean(SettingSupport.class);
        groupManager = context.getBean(GroupManager.class);
        userManager = context.getBean(UserManager.class);
    }
}
