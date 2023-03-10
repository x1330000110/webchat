package com.socket.client.manager;

import cn.hutool.core.util.StrUtil;
import com.socket.client.custom.KeywordSupport;
import com.socket.client.feign.ChatRecordApi;
import com.socket.client.feign.SysUserLogApi;
import com.socket.client.model.GroupPreview;
import com.socket.client.model.SocketMessage;
import com.socket.client.model.SocketUser;
import com.socket.client.model.UserPreview;
import com.socket.core.constant.ChatProperties;
import com.socket.core.custom.SocketRedisManager;
import com.socket.core.mapper.SysGroupMapper;
import com.socket.core.mapper.SysGroupUserMapper;
import com.socket.core.mapper.SysUserMapper;
import com.socket.core.model.base.BaseUser;
import com.socket.core.model.command.impl.PermissionEnum;
import com.socket.core.model.po.*;
import com.socket.core.util.Wss;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ws权限管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionManager implements InitializingBean {
    private final KeywordSupport keywordSupport;
    private final ChatProperties properties;
    private final SocketRedisManager redisManager;
    private final GroupManager groupManager;
    private final UserManager userManager;
    private final SysGroupUserMapper sysGroupUserMapper;
    private final SysGroupMapper sysGroupMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserLogApi sysUserLogApi;
    private final ChatRecordApi chatRecordApi;


    /**
     * 获取聊天室用户（个人资料包含已屏蔽的用户列表）
     *
     * @param self 当前登录的用户
     */
    public List<BaseUser> getUserPreviews(SocketUser self) {
        // 消息发起者
        String suid = self.getGuid();
        // 与此用户关联的最新未读消息
        Map<String, ChatRecord> latest = chatRecordApi.getLatest().getData();
        // 登录记录
        Map<String, SysUserLog> logs = sysUserLogApi.getLatest().getData();
        // 链接数据
        List<BaseUser> previews = new ArrayList<>();
        for (SocketUser user : userManager.values()) {
            UserPreview preview = new UserPreview(user);
            String target = preview.getGuid();
            // 关联日志
            SysUserLog log = logs.get(target);
            if (log != null) {
                preview.setLastTime(log.getCreateTime().getTime());
                preview.setProvince(log.getProvince());
            }
            // 检查未读消息
            int count = redisManager.getUnreadCount(suid, target);
            if (count > 0) {
                ChatRecord unread = latest.get(target);
                if (unread != null) {
                    preview.setPreview(unread);
                    preview.setLastTime(unread.getCreateTime().getTime());
                    preview.setUnreads(Math.min(count, 99));
                }
            }
            // 为自己赋值屏蔽列表
            if (target.equals(suid)) {
                preview.setShields(getShield(self));
            }
            previews.add(preview);
        }
        // 添加群组到列表
        groupManager.forEach((group, value) -> {
            List<String> uids = value.stream().map(SysUser::getGuid).collect(Collectors.toList());
            // 需要在群里
            if (uids.contains(suid)) {
                GroupPreview preview = new GroupPreview(group);
                preview.setIsgroup(true);
                preview.setGuids(uids);
                previews.add(preview);
            }
        });
        return previews;
    }

    /**
     * 获取指定用户屏蔽列表
     *
     * @param user 用户
     * @return 屏蔽列表
     */
    public List<String> getShield(SocketUser user) {
        return redisManager.getShield(user.getGuid());
    }

    /**
     * 检查指定用户禁言情况，若用户被禁言将发送一条系统通知
     */
    public void checkMute(SocketUser user) {
        long time = redisManager.getMuteTime(user.getGuid());
        if (time > 0) {
            user.send(String.valueOf(time), PermissionEnum.MUTE, user);
        }
    }

    /**
     * 检查消息合法性
     *
     * @param user      发起者
     * @param message   消息
     * @param sensitive 敏感关键词检查
     * @return 是否通过
     */
    public boolean verifyMessage(SocketUser user, SocketMessage message, boolean sensitive) {
        String content = message.getContent();
        if (content == null) {
            return true;
        }
        content = StrUtil.sub(content, 0, properties.getMaxMessageLength());
        // 违规字符检查的代替方案
        content = content.replace("<", "&lt;");
        content = content.replace(">", "&gt;");
        if (sensitive && keywordSupport.containsSensitive(content)) {
            user.reject("消息包含敏感关键词，请检查后重新发送", message);
            return false;
        }
        message.setContent(content);
        return true;
    }

    /**
     * 检查指定用户是否被目标屏蔽（优先通过缓存加载）
     */
    public boolean shield(SocketUser source, SocketUser target) {
        return getShield(source).contains(target.getGuid());
    }

    /**
     * 连续发言标记（排除所有者） <br>
     * 10秒内超过一定次数会被禁止一段时间发言
     */
    public void operateMark(SocketUser user) {
        if (!user.isOwner()) {
            long time = TimeUnit.HOURS.toSeconds(properties.getFrequentSpeechMuteTime());
            if (redisManager.incrSpeak(user.getGuid()) > properties.getFrequentSpeechThreshold()) {
                redisManager.setMute(user.getGuid(), time);
                // b结尾 特殊的刷屏标记
                user.send(time + "b", PermissionEnum.MUTE, user);
            }
        }
    }

    /**
     * 检查指定用户是否被禁言
     *
     * @param user 用户
     * @return true被禁言
     */
    public boolean isMute(SocketUser user) {
        return redisManager.getMuteTime(user.getGuid()) > 0;
    }

    /**
     * 检查用户/群组是否存在
     *
     * @param target 目标uid
     * @return 是否存在
     */
    public boolean notHas(String target) {
        if (Wss.isGroup(target)) {
            return groupManager.get(target) == null;
        }
        return userManager.get(target) == null;
    }

    @Override
    public void afterPropertiesSet() {
        // 缓存用户
        List<SysUser> userList = sysUserMapper.selectList(null);
        userList.stream().map(SocketUser::new).forEach(e -> userManager.put(e.getGuid(), e));
        // 缓存群组
        List<SysGroup> sysGroups = sysGroupMapper.selectList(null);
        List<SysGroupUser> groupUsers = sysGroupUserMapper.selectList(null);
        for (SysGroup group : sysGroups) {
            List<SocketUser> collect = groupUsers.stream()
                    .filter(e -> e.getGid().equals(group.getGuid()))
                    .map(SysGroupUser::getUid)
                    .map(userManager::get)
                    .collect(Collectors.toList());
            groupManager.put(group, collect);
        }
    }
}
