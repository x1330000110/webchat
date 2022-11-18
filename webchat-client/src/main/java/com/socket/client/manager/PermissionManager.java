package com.socket.client.manager;

import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.StrUtil;
import com.socket.client.model.UserPreview;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.Callback;
import com.socket.client.model.enums.OnlineState;
import com.socket.client.support.KeywordSupport;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.RedisManager;
import com.socket.webchat.custom.listener.PermissionEvent;
import com.socket.webchat.custom.listener.PermissionListener;
import com.socket.webchat.custom.listener.PermissionOperation;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.SysUserLog;
import com.socket.webchat.model.enums.UserRole;
import com.socket.webchat.service.RecordService;
import com.socket.webchat.service.SysUserLogService;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ws权限管理器
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PermissionManager implements PermissionListener {
    private final KeywordSupport keywordSupport;

    private final SysUserLogService logService;
    private final RecordService recordService;

    private final RedisManager redisManager;
    private final GroupManager groupManager;
    private final UserManager userManager;


    /**
     * 获取聊天室用户（个人资料包含已屏蔽的用户列表）
     *
     * @param self 当前登录的用户
     */
    public Collection<UserPreview> getUserPreviews(WsUser self) {
        // 消息发起者
        String suid = self.getUid();
        // 与此用户关联的最新未读消息
        Map<String, ChatRecord> unreadMessages = recordService.getLatestUnreadMessages(suid);
        // 登录记录
        Map<String, SysUserLog> logs = logService.getUserLogs();
        // 链接数据
        List<UserPreview> previews = new ArrayList<>();
        userManager.values().stream().map(UserPreview::new)
                .peek(preview -> Optional.ofNullable(logs.get(preview.getUid())).ifPresent(log -> {
                    // 关联日志
                    preview.setLastTime(log.getCreateTime().getTime());
                    preview.setRemoteProvince(log.getRemoteProvince());
                })).forEach(preview -> {
                    String target = preview.getUid();
                    // 检查未读消息
                    int count = redisManager.getUnreadCount(suid, target);
                    if (count > 0) {
                        Optional.ofNullable(unreadMessages.get(target)).ifPresent(unread -> {
                            preview.setPreview(unread);
                            preview.setLastTime(unread.getCreateTime().getTime());
                            preview.setUnreads(Math.min(count, 99));
                        });
                    }
                    // 为自己赋值屏蔽列表
                    if (preview.getUid().equals(suid)) {
                        preview.setShields(getShield(self));
                    }
                    previews.add(preview);
                });
        // 添加群组到列表
        groupManager.forEach((group, value) -> {
            List<String> uids = value.stream().map(SysUser::getUid).collect(Collectors.toList());
            // 需要在群里
            if (uids.contains(suid)) {
                UserPreview preview = new UserPreview();
                preview.setIsgroup(true);
                preview.setMembers(uids);
                preview.setUid(group.getGroupId());
                preview.setName(group.getName());
                preview.setOwner(group.getOwner());
                preview.setOnline(OnlineState.ONLINE);
                previews.add(preview);
            }
        });
        return previews;
    }

    /**
     * 检查指定用户禁言情况，若用户被禁言将发送一条系统通知
     */
    public void checkMute(WsUser user) {
        long time = redisManager.getMuteTime(user.getUid());
        if (time > 0) {
            user.send(null, PermissionOperation.MUTE, time);
        }
    }

    /**
     * 检查消息合法性
     *
     * @param wsuser    发起者
     * @param wsmsg     消息
     * @param sensitive 敏感关键词检查
     * @return 是否通过
     */
    public boolean verifyMessage(WsUser wsuser, WsMsg wsmsg, boolean sensitive) {
        String content = wsmsg.getContent();
        if (content == null) {
            return true;
        }
        content = StrUtil.sub(content, 0, Constants.MAX_MESSAGE_LENGTH);
        // 违规字符检查的代替方案
        content = content.replace("<", "&lt;");
        content = content.replace(">", "&gt;");
        if (sensitive && keywordSupport.containsSensitive(content)) {
            wsuser.reject(Callback.SENSITIVE_KEYWORDS, wsmsg);
            return false;
        }
        wsmsg.setContent(content);
        return true;
    }


    /**
     * 检查指定用户是否被目标屏蔽（优先通过缓存加载）
     */
    public boolean shield(WsUser secure, WsUser target) {
        return getShield(secure).contains(target.getUid());
    }

    /**
     * 获取指定用户屏蔽列表
     *
     * @param wsuser 用户
     * @return 屏蔽列表
     */
    public List<String> getShield(WsUser wsuser) {
        return redisManager.getShield(wsuser.getUid());
    }


    /**
     * 连续发言标记（排除所有者） <br>
     * 10秒内超过一定次数会被禁止一段时间发言
     */
    public void operateMark(WsUser user) {
        if (!user.isOwner()) {
            long time = TimeUnit.HOURS.toSeconds(Constants.FREQUENT_SPEECHES_MUTE_TIME);
            if (redisManager.incrSpeak(user.getUid()) > Constants.FREQUENT_SPEECH_THRESHOLD) {
                redisManager.setMute(user.getUid(), time);
                user.send(Callback.BRUSH_SCREEN.format(time), PermissionOperation.MUTE, time);
            }
        }
    }

    /**
     * 检查指定用户是否被禁言
     *
     * @param user 用户
     * @return true被禁言
     */
    public boolean isMute(WsUser user) {
        return redisManager.getMuteTime(user.getUid()) > 0;
    }

    /**
     * 获取消息发送的目标（用户/群组）
     *
     * @param wsmsg 消息
     * @return 目标
     */
    public WsUser getTarget(WsMsg wsmsg) {
        String target = wsmsg.getTarget();
        if (wsmsg.isGroup()) {
            WsUser user = new WsUser();
            SysGroup group = groupManager.getGroup(target);
            user.setUid(group.getGroupId());
            user.setName(group.getName());
            return user;
        }
        return userManager.getUser(target);
    }

    @Override
    public void onPermission(PermissionEvent event) {
        WsUser user = Opt.ofNullable(event.getTarget()).map(userManager::getUser).get();
        String data = event.getData();
        // 解析命令
        switch (event.getOperation()) {
            case ANNOUNCE:
                userManager.sendAll(data, PermissionOperation.ANNOUNCE);
                break;
            case WITHDRAW:
                withdraw(event.getRecord());
                break;
            case ROLE:
                user.setRole(UserRole.of(data));
                userManager.sendAll(PermissionOperation.ROLE, user);
                break;
            case SHIELD:
                userManager.sendAll(PermissionOperation.SHIELD, user);
                break;
            case ALIAS:
                userManager.sendAll(data, PermissionOperation.ALIAS, user);
                break;
            case MUTE:
                userManager.sendAll(data, PermissionOperation.MUTE, user);
                break;
            case LOCK:
                userManager.exit(user, Callback.LOGIN_LIMIT.format(Long.parseLong(data)));
                userManager.sendAll(data, PermissionOperation.LOCK, user);
                break;
            case FOREVER:
                userManager.exit(user, Callback.LIMIT_FOREVER.get());
                userManager.remove(user.getUid());
                break;
            default:
                // ignore
        }
    }

    /**
     * 撤回消息后续处理
     */
    private void withdraw(ChatRecord record) {
        WsUser self = userManager.getUser(record.getUid());
        // 构建消息
        String target = record.getTarget();
        String mid = record.getMid();
        WsMsg wsmsg = new WsMsg(mid, PermissionOperation.WITHDRAW);
        wsmsg.setUid(self.getUid());
        wsmsg.setTarget(target);
        // 目标是群组 通知群组撤回此消息
        if (Wss.isGroup(target)) {
            wsmsg.setData(groupManager.getGroup(target));
            groupManager.sendGroup(wsmsg);
            return;
        }
        // 通知双方撤回此消息
        wsmsg.setData(self);
        userManager.getUser(target).send(wsmsg);
        self.send(wsmsg);
    }
}
