package com.socket.client.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.socket.client.model.UserPreview;
import com.socket.client.model.WsMsg;
import com.socket.client.model.WsUser;
import com.socket.client.model.enums.OnlineState;
import com.socket.client.support.KeywordSupport;
import com.socket.webchat.constant.Constants;
import com.socket.webchat.custom.RedisManager;
import com.socket.webchat.mapper.SysGroupMapper;
import com.socket.webchat.mapper.SysGroupUserMapper;
import com.socket.webchat.mapper.SysUserMapper;
import com.socket.webchat.model.*;
import com.socket.webchat.model.command.impl.PermissEnum;
import com.socket.webchat.service.RecordService;
import com.socket.webchat.service.SysUserLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
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
public class PermissionManager implements InitializingBean {
    private final KeywordSupport keywordSupport;

    private final SysGroupUserMapper sysGroupUserMapper;
    private final SysUserLogService sysUserLogService;
    private final SysGroupMapper sysGroupMapper;
    private final RecordService recordService;
    private final SysUserMapper sysUserMapper;

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
        String suid = self.getGuid();
        // 与此用户关联的最新未读消息
        Map<String, ChatRecord> unreadMessages = recordService.getLatestUnreadMessages(suid);
        // 登录记录
        Map<String, SysUserLog> logs = sysUserLogService.getUserLogs();
        // 链接数据
        List<UserPreview> previews = new ArrayList<>();
        userMap.values().stream().map(UserPreview::new)
                .peek(preview -> Optional.ofNullable(logs.get(preview.getGuid())).ifPresent(log -> {
                    // 关联日志
                    preview.setLastTime(log.getCreateTime().getTime());
                    preview.setRemoteProvince(log.getRemoteProvince());
                })).forEach(preview -> {
                    String target = preview.getGuid();
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
                    if (preview.getGuid().equals(suid)) {
                        preview.setShields(getShield(self));
                    }
                    previews.add(preview);
                });
        // 添加群组到列表
        groupMap.forEach((group, value) -> {
            List<String> uids = value.stream().map(SysUser::getGuid).collect(Collectors.toList());
            // 需要在群里
            if (uids.contains(suid)) {
                UserPreview preview = BeanUtil.copyProperties(group, UserPreview.class);
                preview.setIsgroup(true);
                preview.setGuids(uids);
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
        long time = redisManager.getMuteTime(user.getGuid());
        if (time > 0) {
            user.send(String.valueOf(time), PermissEnum.MUTE, user);
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
        return getShield(secure).contains(target.getGuid());
    }

    /**
     * 获取指定用户屏蔽列表
     *
     * @param wsuser 用户
     * @return 屏蔽列表
     */
    public List<String> getShield(WsUser wsuser) {
        return redisManager.getShield(wsuser.getGuid());
    }


    /**
     * 连续发言标记（排除所有者） <br>
     * 10秒内超过一定次数会被禁止一段时间发言
     */
    public void operateMark(WsUser user) {
        if (!user.isOwner()) {
            long time = TimeUnit.HOURS.toSeconds(Constants.FREQUENT_SPEECHES_MUTE_TIME);
            if (redisManager.incrSpeak(user.getGuid()) > Constants.FREQUENT_SPEECH_THRESHOLD) {
                redisManager.setMute(user.getGuid(), time);
                // b结尾 特殊的刷屏标记
                user.send(time + "b", PermissEnum.MUTE, user);
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
        return redisManager.getMuteTime(user.getGuid()) > 0;
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
            user.setGuid(group.getGuid());
            user.setName(group.getName());
            return user;
        }
        return userMap.getUser(target);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 缓存用户
        List<SysUser> userList = sysUserMapper.selectList(null);
        userList.stream().map(WsUser::new).forEach(e -> userMap.put(e.getGuid(), e));
        // 缓存群组
        List<SysGroup> sysGroups = sysGroupMapper.selectList(null);
        List<SysGroupUser> groupthis = sysGroupUserMapper.selectList(null);
        for (SysGroup group : sysGroups) {
            List<WsUser> collect = groupthis.stream()
                    .filter(e -> e.getGid().equals(group.getGuid()))
                    .map(SysGroupUser::getUid)
                    .map(userMap::getUser)
                    .collect(Collectors.toList());
            groupMap.put(group, collect);
        }
    }
}
