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
import com.socket.webchat.custom.event.PermissionEvent;
import com.socket.webchat.model.ChatRecord;
import com.socket.webchat.model.SysGroup;
import com.socket.webchat.model.SysUser;
import com.socket.webchat.model.SysUserLog;
import com.socket.webchat.model.enums.PermissionEnum;
import com.socket.webchat.model.enums.UserRole;
import com.socket.webchat.service.RecordService;
import com.socket.webchat.service.SysUserLogService;
import com.socket.webchat.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
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
public class PermissionManager {
    private final KeywordSupport keywordSupport;

    private final SysUserLogService logService;
    private final RecordService recordService;

    private final RedisManager redisManager;
    private final WsGroupMap groupMap;
    private final WsUserMap userMap;


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
        userMap.values().stream().map(UserPreview::new)
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
        groupMap.forEach((group, value) -> {
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
            user.send(null, PermissionEnum.MUTE, time);
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
            wsuser.reject("消息包含敏感关键词，请检查后重新发送", wsmsg);
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
                user.send(Callback.BRUSH_SCREEN.format(time), PermissionEnum.MUTE, time);
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
            SysGroup group = groupMap.getGroup(target);
            user.setUid(group.getGroupId());
            user.setName(group.getName());
            return user;
        }
        return userMap.getUser(target);
    }

    /**
     * 权限事件监视器
     */
    @EventListener(PermissionEvent.class)
    public void onPermission(PermissionEvent event) {
        WsUser user = Opt.ofNullable(event.getTarget()).map(userMap::getUser).get();
        String data = event.getData();
        // 解析命令
        switch (event.getOperation()) {
            case ANNOUNCE:
                userMap.sendAll(data, PermissionEnum.ANNOUNCE);
                break;
            case WITHDRAW:
                withdraw(event.getRecord());
                break;
            case ROLE:
                user.setRole(UserRole.of(data));
                userMap.sendAll(PermissionEnum.ROLE, user);
                break;
            case SHIELD:
                userMap.sendAll(PermissionEnum.SHIELD, user);
                break;
            case ALIAS:
                userMap.sendAll(data, PermissionEnum.ALIAS, user);
                break;
            case MUTE:
                userMap.sendAll(data, PermissionEnum.MUTE, user);
                break;
            case LOCK:
                userMap.exit(user, Callback.LOGIN_LIMIT.format(Long.parseLong(data)));
                userMap.sendAll(data, PermissionEnum.LOCK, user);
                break;
            case FOREVER:
                userMap.exit(user, "您已被管理员永久限制登陆");
                userMap.remove(user.getUid());
                break;
            default:
                // ignore
        }
    }

    /**
     * 撤回消息后续处理
     */
    private void withdraw(ChatRecord record) {
        WsUser self = userMap.getUser(record.getUid());
        // 构建消息
        String target = record.getTarget();
        String mid = record.getMid();
        WsMsg wsmsg = new WsMsg(mid, PermissionEnum.WITHDRAW);
        wsmsg.setUid(self.getUid());
        wsmsg.setTarget(target);
        // 目标是群组 通知群组撤回此消息
        if (Wss.isGroup(target)) {
            wsmsg.setData(groupMap.getGroup(target));
            groupMap.sendGroup(wsmsg);
            return;
        }
        // 通知双方撤回此消息
        wsmsg.setData(self);
        userMap.getUser(target).send(wsmsg);
        self.send(wsmsg);
    }
}
